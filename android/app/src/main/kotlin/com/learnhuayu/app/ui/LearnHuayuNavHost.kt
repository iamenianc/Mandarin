package com.learnhuayu.app.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.learnhuayu.app.ui.audio.AudioCheckScreen
import com.learnhuayu.app.ui.session.SessionKind
import com.learnhuayu.app.ui.session.SessionScreen
import com.learnhuayu.core.ui.FeatureDestination

private object Destinations {
    const val HOME = "home"
    const val AUDIO_CHECK = "audio-check"
    const val MODULE = "module"
    const val MODULE_ID = "moduleId"
    const val MODULE_ROUTE = "$MODULE/{$MODULE_ID}"

    const val SESSION = "session"
    const val SESSION_KIND = "kind"
    const val SPEC_ID = "specId"
    const val SESSION_ROUTE = "$SESSION/{$MODULE_ID}/{$SESSION_KIND}/{$SPEC_ID}"

    const val FEATURE = "feature"
    const val FEATURE_ID = "featureId"
    const val FEATURE_ROUTE = "$FEATURE/{$FEATURE_ID}"

    fun module(moduleId: String): String = "$MODULE/$moduleId"

    fun session(moduleId: String, kind: SessionKind, specId: String): String = "$SESSION/$moduleId/${kind.routeValue}/$specId"

    fun feature(featureId: String): String = "$FEATURE/$featureId"
}

@Composable
fun LearnHuayuNavHost(featureDestinations: Set<FeatureDestination> = emptySet()) {
    val navController = rememberNavController()
    val featuresById = remember(featureDestinations) { featureDestinations.associateBy { it.id } }

    NavHost(navController = navController, startDestination = Destinations.HOME) {
        composable(Destinations.HOME) {
            HomeScreen(
                onModuleClick = { moduleId -> navController.navigate(Destinations.module(moduleId)) },
                onAudioCheckClick = { navController.navigate(Destinations.AUDIO_CHECK) },
                onFeatureClick = { featureId -> navController.navigate(Destinations.feature(featureId)) },
            )
        }
        composable(Destinations.AUDIO_CHECK) {
            AudioCheckScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Destinations.MODULE_ROUTE,
            arguments = listOf(navArgument(Destinations.MODULE_ID) { type = NavType.StringType }),
        ) { entry ->
            ModuleScreen(
                moduleId = entry.arguments?.getString(Destinations.MODULE_ID).orEmpty(),
                onSpecClick = { kind, specId ->
                    navController.navigate(Destinations.session(entry.arguments?.getString(Destinations.MODULE_ID).orEmpty(), kind, specId))
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Destinations.SESSION_ROUTE,
            arguments = listOf(
                navArgument(Destinations.MODULE_ID) { type = NavType.StringType },
                navArgument(Destinations.SESSION_KIND) { type = NavType.StringType },
                navArgument(Destinations.SPEC_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            SessionScreen(
                moduleId = entry.arguments?.getString(Destinations.MODULE_ID).orEmpty(),
                kindValue = entry.arguments?.getString(Destinations.SESSION_KIND).orEmpty(),
                specId = entry.arguments?.getString(Destinations.SPEC_ID).orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Destinations.FEATURE_ROUTE,
            arguments = listOf(navArgument(Destinations.FEATURE_ID) { type = NavType.StringType }),
        ) { entry ->
            val destination = featuresById[entry.arguments?.getString(Destinations.FEATURE_ID).orEmpty()]
            if (destination == null) {
                Text(text = "Feature not found.")
            } else {
                destination.Content(onBack = { navController.popBackStack() })
            }
        }
    }
}
