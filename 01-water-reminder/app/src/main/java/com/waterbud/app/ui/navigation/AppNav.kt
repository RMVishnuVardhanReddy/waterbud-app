package com.waterbud.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.waterbud.app.ui.history.HistoryScreen
import com.waterbud.app.ui.home.HomeScreen
import com.waterbud.app.ui.onboarding.OnboardingScreen
import com.waterbud.app.ui.settings.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

@Composable
fun AppNav(
    startOnboarding: Boolean,
    onUpgrade: () -> Unit,
    onLeaveDetail: () -> Unit,
    onWatchRewardedForThemes: (onDone: (Boolean) -> Unit) -> Unit,
    onEnableHealthSync: (onResult: (Boolean) -> Unit) -> Unit
) {
    val nav = rememberNavController()

    // Returning Home from a detail screen is a "natural break" — the only place
    // we may (rarely, capped) show an interstitial.
    fun backToHome() { onLeaveDetail(); nav.popBackStack() }

    NavHost(
        navController = nav,
        startDestination = if (startOnboarding) Routes.ONBOARDING else Routes.HOME
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onDone = {
                nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenHistory = { nav.navigate(Routes.HISTORY) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.HISTORY) { HistoryScreen(onBack = { backToHome() }) }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { backToHome() },
                onUpgrade = onUpgrade,
                onWatchRewardedForThemes = onWatchRewardedForThemes,
                onEnableHealthSync = onEnableHealthSync
            )
        }
    }
}
