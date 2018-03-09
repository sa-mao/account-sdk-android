/*
 * Copyright (c) 2018 Schibsted Products & Technology AS. Licensed under the terms of the MIT license. See LICENSE in the project root.
 */

package com.schibsted.account.ui.login

import com.schibsted.account.engine.controller.PasswordlessController
import com.schibsted.account.engine.input.Agreements
import com.schibsted.account.engine.input.Credentials
import com.schibsted.account.engine.input.Identifier
import com.schibsted.account.engine.input.RequiredFields
import com.schibsted.account.engine.input.VerificationCode
import com.schibsted.account.engine.integration.InputProvider
import com.schibsted.account.network.response.AgreementLinksResponse
import com.schibsted.account.ui.UiConfiguration
import com.schibsted.account.ui.login.flow.password.FlowSelectionListener
import com.schibsted.account.ui.login.screen.identification.IdentificationPresenter
import com.schibsted.account.ui.login.screen.identification.ui.AbstractIdentificationFragment
import com.schibsted.account.ui.login.screen.identification.ui.EmailIdentificationFragment
import com.schibsted.account.ui.login.screen.identification.ui.MobileIdentificationFragment
import com.schibsted.account.ui.login.screen.inbox.InboxFragment
import com.schibsted.account.ui.login.screen.information.RequiredFieldsFragment
import com.schibsted.account.ui.login.screen.information.RequiredFieldsPresenter
import com.schibsted.account.ui.login.screen.password.PasswordFragment
import com.schibsted.account.ui.login.screen.password.PasswordPresenter
import com.schibsted.account.ui.login.screen.term.TermsFragment
import com.schibsted.account.ui.login.screen.term.TermsPresenter
import com.schibsted.account.ui.login.screen.verification.VerificationFragment
import com.schibsted.account.ui.login.screen.verification.VerificationPresenter
import com.schibsted.account.ui.ui.BaseFragment

class FragmentProvider(private val uiConfiguration: UiConfiguration) {

    fun getOrCreateIdentificationFragment(currentFragment: BaseFragment?, provider: InputProvider<Identifier>? = null,
        identifierType: String, flowSelectionListener: FlowSelectionListener? = null): BaseFragment {

        return getFragment<AbstractIdentificationFragment>(currentFragment, {
            it.setPresenter(IdentificationPresenter(it, provider, flowSelectionListener))
        }, {
            if (identifierType == Identifier.IdentifierType.SMS.value) {
                MobileIdentificationFragment.newInstance(uiConfiguration)
            } else {
                EmailIdentificationFragment.newInstance(uiConfiguration)
            }
        })
    }

    fun getOrCreatePasswordFragment(currentFragment: BaseFragment?, provider: InputProvider<Credentials>,
        currentIdentifier: Identifier, userAvailable: Boolean): BaseFragment {
        return getFragment(currentFragment, {
            it.setPresenter(PasswordPresenter(it, provider))
        }, {
            PasswordFragment.newInstance(currentIdentifier, userAvailable, uiConfiguration)
        })
    }

    fun getOrCreateInboxFragment(currentFragment: BaseFragment?, currentIdentifier: Identifier): BaseFragment {
        return currentFragment as? InboxFragment ?: InboxFragment.newInstance(currentIdentifier)
    }

    fun getOrCreateTermsFragment(currentFragment: BaseFragment?, provider: InputProvider<Agreements>, userAvailable: Boolean, agreementLinks: AgreementLinksResponse): BaseFragment {
        return getFragment(currentFragment, {
            it.setPresenter(TermsPresenter(it, provider))
        }, {
            TermsFragment.newInstance(uiConfiguration, userAvailable, agreementLinks)
        })
    }

    fun getOrCreateRequiredFieldsFragment(currentFragment: BaseFragment?, provider: InputProvider<RequiredFields>, fields: Set<String>): BaseFragment {
        return getFragment(currentFragment, {
            it.setPresenter(RequiredFieldsPresenter(it, provider))
            it.missingField = fields
        }, {
            RequiredFieldsFragment.newInstance()
        })
    }

    fun getOrCreateVerificationScreen(currentFragment: BaseFragment?, provider: InputProvider<VerificationCode>,
        identifier: Identifier, passwordlessController: PasswordlessController): BaseFragment {
        return getFragment<VerificationFragment>(currentFragment, {
            it.setPresenter(VerificationPresenter(it, provider))
            it.setPasswordlessController(passwordlessController)
        }, { VerificationFragment.newInstance(identifier) })
    }

    private inline fun <reified T> getFragment(existingFragment: BaseFragment?,
        applyTo: (T) -> Unit, create: () -> T): T {
        return if (existingFragment is T) {
            applyTo(existingFragment)
            existingFragment
        } else {
            val fragment = create()
            applyTo(fragment)
            fragment
        }
    }
}