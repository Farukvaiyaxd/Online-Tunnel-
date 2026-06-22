package com.tritunnel.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tritunnel.app.data.VpnConfig
import com.tritunnel.app.ui.screen.AddServerScreen
import com.tritunnel.app.ui.screen.DashboardScreen
import com.tritunnel.app.ui.screen.ServerListScreen
import com.tritunnel.app.ui.screen.SettingsScreen
import com.tritunnel.app.ui.screen.SplashScreen
import com.tritunnel.app.ui.theme.CyberBg
import com.tritunnel.app.ui.theme.CyberSurface
import com.tritunnel.app.ui.theme.DividerColor
import com.tritunnel.app.ui.theme.NeonCyan
import com.tritunnel.app.ui.theme.TextSecondary
import com.tritunnel.app.ui.theme.TriTunnelTheme
import com.tritunnel.app.ui.viewmodel.VpnViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle deep link: tritunnel://add?config=JSON
        val deepLinkConfig = intent?.data?.let { uri ->
            if (uri.scheme == "tritunnel" && uri.host == "add") {
                runCatching { VpnConfig.fromJson(org.json.JSONObject(uri.getQueryParameter("config") ?: "{}")) }.getOrNull()
            } else null
        }

        setContent {
            TriTunnelTheme {
                val vm: VpnViewModel = viewModel()
                deepLinkConfig?.let { vm.addOrUpdateServer(it) }
                AppNavHost(vm)
            }
        }
    }
}

@Composable
private fun AppNavHost(vm: VpnViewModel) {
    val nav = rememberNavController()
    var editTarget by remember { mutableStateOf<VpnConfig?>(null) }
    var splashDone by rememberSaveable { mutableStateOf(false) }

    if (!splashDone) {
        SplashScreen(onFinished = { splashDone = true })
        return
    }

    val bottomItems = listOf(
        Triple("dashboard", Icons.Default.Dashboard, "Home"),
        Triple("servers", Icons.Default.Dns, "Servers"),
        Triple("settings", Icons.Default.Settings, "Settings"),
    )
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBottom = currentRoute in listOf("dashboard", "servers", "settings")

    Scaffold(
        containerColor = CyberBg,
        bottomBar = {
            if (showBottom) {
                NavigationBar(
                    containerColor = CyberSurface,
                    tonalElevation = 0.dp,
                ) {
                    bottomItems.forEach { (route, icon, label) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                nav.navigate(route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            },
                            icon = { Icon(icon, null, modifier = Modifier.size(22.dp)) },
                            label = { Text(label, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = NeonCyan.copy(alpha = 0.12f),
                            )
                        )
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            NavHost(
                navController = nav,
                startDestination = "dashboard",
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(250)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(250)) },
                popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(250)) },
                popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(250)) },
            ) {
                composable("dashboard") { DashboardScreen(vm) }
                composable("servers") {
                    ServerListScreen(
                        vm = vm,
                        onAddServer = { editTarget = null; nav.navigate("add_server") },
                        onEditServer = { editTarget = it; nav.navigate("add_server") },
                    )
                }
                composable("settings") { SettingsScreen(vm) }
                composable("add_server") {
                    AddServerScreen(
                        vm = vm,
                        existingConfig = editTarget,
                        onBack = { nav.popBackStack() }
                    )
                }
            }
        }
    }
}
