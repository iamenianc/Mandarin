package com.learnhuayu.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

private object Destinations {
    const val HOME = "home"
    const val MODULE = "module"
    const val MODULE_ID = "moduleId"
    const val MODULE_ROUTE = "$MODULE/{$MODULE_ID}"

    fun module(moduleId: String): String = "$MODULE/$moduleId"
}

@Composable
fun LearnHuayuNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Destinations.HOME) {
        composable(Destinations.HOME) {
            HomeScreen(
                onModuleClick = { moduleId -> navController.navigate(Destinations.module(moduleId)) },
            )
        }
        composable(
            route = Destinations.MODULE_ROUTE,
            arguments = listOf(navArgument(Destinations.MODULE_ID) { type = NavType.StringType }),
        ) { entry ->
            ModuleScreen(
                moduleId = entry.arguments?.getString(Destinations.MODULE_ID).orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
