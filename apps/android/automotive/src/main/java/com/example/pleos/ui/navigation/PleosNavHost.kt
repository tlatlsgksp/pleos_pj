package com.example.pleos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pleos.ui.feature.charge.ChargeConfirmRoute
import com.example.pleos.ui.feature.charge.ChargeConfirmViewModel
import com.example.pleos.ui.feature.main.MainRoute
import com.example.pleos.ui.feature.skin.SkinRoute

/** MAIN holds the three tabs; the rest are second-depth screens with a back button. */
object Routes {
    const val MAIN = "main"
    const val SKIN = "skin"
    const val CHARGE = "charge/{${ChargeConfirmViewModel.ARG_RECORD_ID}}"

    fun charge(recordId: String) = "charge/$recordId"
}

@Composable
fun PleosNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainRoute(
                onOpenCharge = { id -> navController.navigate(Routes.charge(id)) { launchSingleTop = true } },
                onOpenSkin = { navController.navigate(Routes.SKIN) { launchSingleTop = true } },
            )
        }
        composable(Routes.SKIN) {
            SkinRoute(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.CHARGE,
            arguments = listOf(navArgument(ChargeConfirmViewModel.ARG_RECORD_ID) { type = NavType.StringType }),
        ) {
            ChargeConfirmRoute(onClose = { navController.popBackStack() })
        }
    }
}
