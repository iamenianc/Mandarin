package com.learnhuayu.core.ui

import androidx.compose.runtime.Composable

/**
 * A standalone screen a feature module contributes to the shell. Implementations are
 * discovered through DI multibinding, listed on Home, and rendered by the shell on the
 * `feature/{id}` route. The contract carries only an identity and a composable, so
 * `:core:ui` stays a plain design system with no Hilt or app knowledge (ADR 0007).
 */
interface FeatureDestination {

    /** Stable identifier used in the `feature/{id}` route; unique across destinations. */
    val id: String

    /** English or pinyin title shown on the Home card and the feature's app bar. */
    val title: String

    @Composable
    fun Content(onBack: () -> Unit)
}
