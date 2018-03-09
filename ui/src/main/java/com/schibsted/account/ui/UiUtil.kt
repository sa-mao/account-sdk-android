/*
 * Copyright (c) 2018 Schibsted Products & Technology AS. Licensed under the terms of the MIT license. See LICENSE in the project root.
 */

package com.schibsted.account.ui

import com.schibsted.account.common.tracking.TrackingData
import com.schibsted.account.engine.input.Identifier
import com.schibsted.account.ui.login.screen.LoginScreen

object UiUtil {
    @JvmStatic
    fun getTrackingScreen(loginScreen: LoginScreen): TrackingData.Screen? = when (loginScreen) {
        LoginScreen.IDENTIFICATION_SCREEN -> TrackingData.Screen.IDENTIFICATION
        LoginScreen.PASSWORD_SCREEN -> TrackingData.Screen.PASSWORD
        LoginScreen.VERIFICATION_SCREEN -> TrackingData.Screen.VERIFICATION_CODE
        LoginScreen.TC_SCREEN -> TrackingData.Screen.AGREEMENTS
        LoginScreen.REQUIRED_FIELDS_SCREEN -> TrackingData.Screen.REQUIRED_FIELDS
        LoginScreen.CHECK_INBOX_SCREEN -> TrackingData.Screen.ACCOUNT_VERIFICATION
        else -> null
    }

    @JvmStatic
    fun getTrackerIdType(idType: Identifier.IdentifierType): TrackingData.IdentifierType = when (idType) {
        Identifier.IdentifierType.EMAIL -> TrackingData.IdentifierType.EMAIL
        Identifier.IdentifierType.SMS -> TrackingData.IdentifierType.PHONE
    }
}