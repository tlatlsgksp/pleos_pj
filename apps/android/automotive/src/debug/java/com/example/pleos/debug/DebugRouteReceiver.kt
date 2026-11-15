package com.example.pleos.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.pleos.di.ApplicationScope
import com.example.pleos.domain.gateway.NavigationGateway
import com.example.pleos.domain.model.Place
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Debug-only: sets or cancels a navigation route through the app's own NavigationGateway, so the
 * charger-waypoint flow can be shown and checked without typing into the navigation app.
 *
 * ```
 * adb shell am broadcast -n com.example.pleos/.debug.DebugRouteReceiver  *   -a com.example.pleos.debug.SET_ROUTE --es name 대전역 --ef lat 36.3322 --ef lng 127.4342
 * adb shell am broadcast -n com.example.pleos/.debug.DebugRouteReceiver -a com.example.pleos.debug.CANCEL_ROUTE
 * ```
 */
@AndroidEntryPoint
class DebugRouteReceiver : BroadcastReceiver() {

    @Inject lateinit var navigation: NavigationGateway

    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_SET_ROUTE -> {
                        val place = Place(
                            name = intent.getStringExtra("name") ?: "대전역",
                            latitude = intent.getFloatExtra("lat", 36.3322f).toDouble(),
                            longitude = intent.getFloatExtra("lng", 127.4342f).toDouble(),
                        )
                        navigation.setDestination(place)
                        Log.d(TAG, "route requested to $place")
                    }
                    ACTION_CANCEL_ROUTE -> navigation.cancelRoute()
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "DebugRoute"
        const val ACTION_SET_ROUTE = "com.example.pleos.debug.SET_ROUTE"
        const val ACTION_CANCEL_ROUTE = "com.example.pleos.debug.CANCEL_ROUTE"
    }
}
