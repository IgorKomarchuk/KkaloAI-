package com.kkaloai.app.util

import com.kkaloai.app.data.billing.BillingManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for tier-gated features.
 *
 * Tiers:
 *  - FREE       — base scan + manual log; daily AI scan limit
 *  - PREMIUM    — unlimited AI scans, weekly report, planner basic
 *  - PREMIUM_PLUS — recipe/menu/fridge scanners, PDF export, advanced stats
 *  - LIFETIME   — same as PREMIUM_PLUS, never expires
 *
 * The exact mapping of `BillingManager` SKUs → tiers lives here so feature checks
 * stay declarative inside ViewModels: `if (gates.canUseRecipeScanner.value) ...`.
 */
enum class Tier { FREE, PREMIUM, PREMIUM_PLUS, LIFETIME }

@Singleton
class FeatureGates @Inject constructor(
    private val billing: BillingManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val tier: StateFlow<Tier> = combine(
        billing.isSubscribed,
        billing.isPremiumPlus,
        billing.isLifetime
    ) { subscribed, plus, lifetime ->
        when {
            lifetime -> Tier.LIFETIME
            plus -> Tier.PREMIUM_PLUS
            subscribed -> Tier.PREMIUM
            else -> Tier.FREE
        }
    }.stateIn(scope, SharingStarted.Eagerly, Tier.FREE)

    val isAnyPaid: StateFlow<Boolean> = tier.map { it != Tier.FREE }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val canUsePlannerChat: StateFlow<Boolean> = tier.map { it != Tier.FREE }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val canUseAdvancedStats: StateFlow<Boolean> = tier.map {
        it == Tier.PREMIUM_PLUS || it == Tier.LIFETIME
    }.stateIn(scope, SharingStarted.Eagerly, false)

    val canUseRecipeScanner: StateFlow<Boolean> = tier.map {
        it == Tier.PREMIUM_PLUS || it == Tier.LIFETIME
    }.stateIn(scope, SharingStarted.Eagerly, false)

    val canUseMenuScanner: StateFlow<Boolean> = canUseRecipeScanner
    val canUseFridgeScanner: StateFlow<Boolean> = canUseRecipeScanner
    val canExportPdf: StateFlow<Boolean> = canUseRecipeScanner

    /**
     * Soft daily AI scan limit for FREE tier — UI checks before calling Gemini.
     * 0 means unlimited.
     */
    fun dailyScanLimit(currentTier: Tier): Int = when (currentTier) {
        Tier.FREE -> 5
        else -> 0
    }
}
