/*
 * Copyright (c) 2018 Schibsted Products & Technology AS. Licensed under the terms of the MIT license. See LICENSE in the project root.
 */

package com.schibsted.account.session.event

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager

class BroadcastManager(context: Context) {
    private val localBroadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)

    fun broadcast(intent: Intent) {
        localBroadcastManager.sendBroadcast(intent)
    }

    fun registerReceiver(broadcastReceiver: BroadcastReceiver, intentFilter: IntentFilter) {
        localBroadcastManager.registerReceiver(broadcastReceiver, intentFilter)
    }

    fun unregisterReceiver(broadcastReceiver: BroadcastReceiver) {
        localBroadcastManager.unregisterReceiver(broadcastReceiver)
    }
}