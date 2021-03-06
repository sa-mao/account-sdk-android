/*
 * Copyright (c) 2018 Schibsted Products & Technology AS. Licensed under the terms of the MIT license. See LICENSE in the project root.
 */

package com.schibsted.account.engine.operation

import com.schibsted.account.ClientConfiguration
import com.schibsted.account.model.ClientToken
import com.schibsted.account.model.error.NetworkError
import com.schibsted.account.network.NetworkCallback
import com.schibsted.account.network.ServiceHolder
import com.schibsted.account.network.response.ApiContainer
import com.schibsted.account.network.response.ClientInfo

/**
 * A task to get client credentials for a SPiD client
 */
class ClientInfoOperation constructor(
    private val failure: (error: NetworkError) -> Unit,
    private val success: (token: ClientInfo) -> Unit
) {

    init {
        ClientTokenOperation({ failure(it) },
                { token: ClientToken ->
                    ServiceHolder.clientService.getClientInfo(token, ClientConfiguration.get().clientId)
                            .enqueue(object : NetworkCallback<ApiContainer<ClientInfo>>("Retrieving client information") {
                                override fun onSuccess(result: ApiContainer<ClientInfo>) {
                                    success(result.data)
                                }

                                override fun onError(error: NetworkError) {
                                    failure(error)
                                }
                            })
                }
        )
    }
}
