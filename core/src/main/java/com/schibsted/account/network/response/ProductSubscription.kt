package com.schibsted.account.network.response

data class ProductSubscription(
    val name: String,
    val code: Int,
    val data: Data
) {
    data class Data(
        val productId: String,
        val result: Boolean
    )
}