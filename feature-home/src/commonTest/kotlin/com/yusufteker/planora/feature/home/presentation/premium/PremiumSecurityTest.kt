package com.yusufteker.planora.feature.home.presentation.premium

import com.yusufteker.planora.core.preferences.SessionPreferences
import com.yusufteker.planora.feature.home.util.createTestSessionPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Premium Security and Expiration Verification Test Suite.
 *
 * Verifies that:
 * 1. Future subscription expiration timestamps allow Premium access.
 * 2. Past/expired subscription timestamps IMMEDIATELY revoke Premium access without needing profile navigation.
 * 3. Expired states trigger auto-downgrades in local session preferences.
 * 4. Infinite/lifetime subscriptions (null expiration date) remain active.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PremiumSecurityTest {

    private lateinit var sessionPreferences: SessionPreferences

    @BeforeTest
    fun setUp() {
        sessionPreferences = createTestSessionPreferences()
    }

    @Test
    fun futureExpirationTimestamp_keepsPremiumActive() = runTest {
        // Expiration in the far future (Year 2099)
        val futureUntil = "2099-12-31T23:59:59Z"
        sessionPreferences.setPremium(isPremium = true, premiumUntil = futureUntil)

        assertTrue(sessionPreferences.isPremium())
        assertTrue(sessionPreferences.isPremiumFlow.first())
        assertEquals(futureUntil, sessionPreferences.getPremiumUntil())
    }

    @Test
    fun pastExpirationTimestamp_immediatelyRevokesPremiumAccess() = runTest {
        // Expiration in the past (Year 2020)
        val pastUntil = "2020-01-01T00:00:00Z"
        sessionPreferences.setPremium(isPremium = true, premiumUntil = pastUntil)

        // isPremiumFlow must immediately report false
        assertFalse(sessionPreferences.isPremiumFlow.first())

        // isPremium() must return false and auto-downgrade the local state
        assertFalse(sessionPreferences.isPremium())
    }

    @Test
    fun lifetimeSubscription_remainsActive() = runTest {
        // null expiration timestamp represents lifetime or indefinite access
        sessionPreferences.setPremium(isPremium = true, premiumUntil = null)

        assertTrue(sessionPreferences.isPremium())
        assertTrue(sessionPreferences.isPremiumFlow.first())
    }

    @Test
    fun nonPremiumUser_returnsFalse() = runTest {
        sessionPreferences.setPremium(isPremium = false, premiumUntil = null)

        assertFalse(sessionPreferences.isPremium())
        assertFalse(sessionPreferences.isPremiumFlow.first())
    }
}
