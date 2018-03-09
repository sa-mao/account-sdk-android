/*
 * Copyright (c) 2018 Schibsted Products & Technology AS. Licensed under the terms of the MIT license. See LICENSE in the project root.
 */

package com.schibsted.account.session

import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.WorkerThread
import com.schibsted.account.ClientConfiguration
import com.schibsted.account.common.util.Logger
import com.schibsted.account.engine.integration.ResultCallback
import com.schibsted.account.engine.integration.ResultCallbackData
import com.schibsted.account.model.UserId
import com.schibsted.account.model.UserToken
import com.schibsted.account.model.error.ClientError
import com.schibsted.account.model.error.NetworkError
import com.schibsted.account.network.AuthInterceptor
import com.schibsted.account.network.InfoInterceptor
import com.schibsted.account.network.NetworkCallback
import com.schibsted.account.network.ServiceHolder
import com.schibsted.account.network.response.TokenResponse
import com.schibsted.account.session.event.BroadcastEvent
import com.schibsted.account.session.event.EventManager
import okhttp3.OkHttpClient

/**
 * Represents a user and the actions a user can take. Actions are grouped under _auth_, _agreements_ and _profile_,
 */
class User(token: UserToken, val isPersistable: Boolean) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readParcelable<TokenResponse>(TokenResponse::class.java.classLoader),
            parcel.readInt() != 0)

    @Volatile
    internal var token: UserToken? = token
        private set(value) {
            if (value == null) {
                ServiceHolder.resetServices()
            }
            field = value
        }

    val userId: UserId = UserId.fromTokenResponse(token)

    fun isActive(): Boolean = token != null

    val auth = Auth(this)

    val agreements = Agreements(this)

    val profile = Profile(this)

    /**
     * Destroys the current session and removes it's access tokens from SPiD. Attempting to use this
     * session afterwards will cause errors
     * @param callback A callback with the result of the logout
     */
    fun logout(callback: ResultCallback?) {
        val token = this.token
        if (token != null) {
            EventManager.broadcast(BroadcastEvent.LogoutEvent(userId))
            ServiceHolder.userService(this).logout(token)
                    .enqueue(object : NetworkCallback<Unit>("Logging out user") {
                        override fun onError(error: NetworkError) {
                            callback?.onError(error.toClientError())
                            this@User.token = null
                        }

                        override fun onSuccess(result: Unit) {
                            callback?.onSuccess()
                            this@User.token = null
                        }
                    })
        } else {
            callback?.onError(ClientError(ClientError.ErrorType.INVALID_STATE, "User already logged out"))
        }
    }

    /**
     * Bind this session to an [OkHttpClient]. This will add an interceptor and override any
     * authenticators already defined. Any requests not matching the host will be denied.
     * @param builder An instance of the [OkHttpClient.Builder] to use
     * @param urls A list of urls the [OkHttpClient] will be used for. This will match sub-paths as well
     * @return An [OkHttpClient.Builder] to which the authenticator and interceptor are attached
     */
    @Suppress("MemberVisibilityCanBePrivate", "Unused")
    fun bind(builder: OkHttpClient.Builder, urls: List<String>): OkHttpClient.Builder {
        return bind(builder, urls, false, false)
    }

    /**
     * Bind this session to an [OkHttpClient]. This will add an interceptor and override any
     * authenticators already defined. Any requests not matching the host will be denied.
     * @param builder An instance of the [OkHttpClient.Builder] to use
     * @param urls A list of urls the [OkHttpClient] will be used for. This will match sub-paths as well
     * @param allowNonHttps By default, non-HTTPS requests are denied. Setting this to true will override this.
     * This is not recommended and is done at your own risk.
     * @param allowNonWhitelistedDomains By default, requests to non-whitelisted domains is not allowed. Set this
     * to true to override that.
     * @return An [OkHttpClient.Builder] to which the authenticator and interceptor are attached
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun bind(builder: OkHttpClient.Builder, urls: List<String>, allowNonHttps: Boolean, allowNonWhitelistedDomains: Boolean): OkHttpClient.Builder {
        return builder
                .addInterceptor(AuthInterceptor(this, urls, allowNonHttps, allowNonWhitelistedDomains))
                .addInterceptor(InfoInterceptor(false))
    }

    @WorkerThread
    internal fun refreshToken(): Boolean {
        val token = this.token
        if (token == null) {
            Logger.warn(Logger.DEFAULT_TAG, { "Attempting to refresh token, but user is logged out" })
            return false
        }

        if (token.refreshToken.isEmpty()) {
            this.token = null
            Logger.warn(Logger.DEFAULT_TAG, { "Attempting to refresh token, but the refresh token is empty. Clearing corrupt token" })
            return false
        }

        Logger.verbose(Logger.DEFAULT_TAG, { "Refreshing user token" })
        val resp = ServiceHolder.oAuthService().refreshToken(ClientConfiguration.get().clientId,
                ClientConfiguration.get().clientSecret, token.refreshToken).execute()

        return if (resp.isSuccessful) {
            this.token = requireNotNull(resp.body(), { "Unable to parse token from successful response" })
            Logger.verbose(Logger.DEFAULT_TAG, { "Refreshing user token was successful" })
            EventManager.broadcast(BroadcastEvent.RefreshEvent(userId))
            true
        } else {
            Logger.verbose(Logger.DEFAULT_TAG, { "User token refreshing failed" })
            if (listOf(400, 401, 403).contains(resp.code())) {
                Logger.verbose(Logger.DEFAULT_TAG, { "Logging out user" })
                ServiceHolder.userService(this).logout(token).execute()
                this@User.token = null

                EventManager.broadcast(BroadcastEvent.LogoutEvent(userId))
            }
            false
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(token, flags)
        parcel.writeInt(if (this.isPersistable) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<User> {
            override fun createFromParcel(parcel: Parcel): User = User(parcel)

            override fun newArray(size: Int): Array<User?> = arrayOfNulls(size)
        }

        /**
         * @param code The session code to create the user from
         * @param redirectUri The redirect URI. Must be found in self service
         * @param isPersistable If the user can be persisted or not. The user's wishes must be respected to be GDPR compliant
         * @param callback The callback to which we provide the User
         */
        @JvmStatic
        fun fromSessionCode(code: String, redirectUri: String, isPersistable: Boolean, callback: ResultCallbackData<User>) {
            val conf = ClientConfiguration.get()
            ServiceHolder.oAuthService().tokenFromAuthCode(conf.clientId, conf.clientSecret, code, redirectUri)
                    .enqueue(NetworkCallback.lambda("Resuming session from session code",
                            { callback.onError(it.toClientError()) },
                            { callback.onSuccess(User(it, isPersistable)) }
                    ))
        }
    }
}