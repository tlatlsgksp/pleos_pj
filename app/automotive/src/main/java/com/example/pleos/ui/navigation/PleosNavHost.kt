package com.example.pleos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pleos.ui.feature.dashboard.DashboardRoute

object Routes {
    const val DASHBOARD = "dashboard"
}

@Composable
fun PleosNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) { DashboardRoute() }
    }
}
