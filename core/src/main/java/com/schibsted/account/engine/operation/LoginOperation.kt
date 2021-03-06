/*
 * Copyright (c) 2018 Schibsted Products & Technology AS. Licensed under the terms of the MIT license. See LICENSE in the project root.
 */

package com.schibsted.account.engine.operation

import com.schibsted.account.ClientConfiguration
import com.schibsted.account.engine.input.Credentials
import com.schibsted.account.model.UserToken
import com.schibsted.account.model.error.NetworkError
import com.schibsted.account.network.NetworkCallback
import com.schibsted.account.network.OIDCScope
import com.schibsted.account.network.ServiceHolder
import com.schibsted.account.network.response.TokenResponse

/**
 * Task to request user credentials and signup with SPiD using these
 */
internal class LoginOperation(
    credentials: Credentials,
    @OIDCScope scopes: Array<String>,
    failure: (NetworkError) -> Unit,
    success: (UserToken) -> Unit
) {

    init {
        ServiceHolder.oAuthService.tokenFromPassword(ClientConfiguration.get().clientId,
                ClientConfiguration.get().clientSecret, credentials.identifier.identifier, credentials.password, *scopes)
                .enqueue(object : NetworkCallback<TokenResponse>("Identifying with username and password in LoginOperation") {
                    override fun onError(error: NetworkError) {
                        failure(error)
                    }

                    override fun onSuccess(result: TokenResponse) {
                        success(result)
                    }
                })
    }
}
