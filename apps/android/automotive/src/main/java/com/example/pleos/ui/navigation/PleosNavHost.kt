package com.example.pleos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pleos.ui.feature.care.CareRoute
import com.example.pleos.ui.feature.charge.ChargeConfirmRoute
import com.example.pleos.ui.feature.charge.ChargeConfirmViewModel
import com.example.pleos.ui.feature.dashboard.DashboardRoute

object Routes {
    const val DASHBOARD = "dashboard"
    const val CARE = "care"
    const val CHARGE = "charge/{${ChargeConfirmViewModel.ARG_RECORD_ID}}"

    fun charge(recordId: String) = "charge/$recordId"
}

@Composable
fun PleosNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardRoute(
                onOpenCare = { navController.navigate(Routes.CARE) { launchSingleTop = true } },
                onOpenCharge = { id -> navController.navigate(Routes.charge(id)) { launchSingleTop = true } },
            )
        }
        composable(Routes.CARE) {
            CareRoute(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.CHARGE,
            arguments = listOf(navArgument(ChargeConfirmViewModel.ARG_RECORD_ID) { type = NavType.StringType }),
        ) {
            ChargeConfirmRoute(onClose = { navController.popBackStack() })
        }
    }
}
