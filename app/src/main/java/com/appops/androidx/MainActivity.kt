package com.appops.androidx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.appops.androidx.privilege.PrivilegeManager
import com.appops.androidx.repository.AppRepository
import com.appops.androidx.ui.AppListScreen
import com.appops.androidx.ui.AppOpsScreen
import com.appops.androidx.ui.CodeEditorScreen
import com.appops.androidx.ui.SettingsScreen
import com.appops.androidx.ui.TerminalScreen
import com.appops.androidx.ui.theme.AppOpsManagerTheme
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    
    private val appRepository by lazy { AppRepository(this) }
    private val SHIZUKU_CODE = 1001
    
    private val shizukuListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_CODE && grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // Permission granted
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (PrivilegeManager.isShizukuAvailable()) {
            Shizuku.addRequestPermissionResultListener(shizukuListener)
            PrivilegeManager.checkShizukuPermission(SHIZUKU_CODE)
        }

        setContent {
            AppOpsManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(appRepository)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (PrivilegeManager.isShizukuAvailable()) {
            Shizuku.removeRequestPermissionResultListener(shizukuListener)
        }
    }
}

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Main : BottomNavItem("main", "Main", Icons.Default.Person)
    object Work : BottomNavItem("work", "Work", Icons.Default.Build)
    object Private : BottomNavItem("private", "Private", Icons.AutoMirrored.Filled.List)
    object System : BottomNavItem("system", "System", Icons.Default.Lock)
    object Settings : BottomNavItem("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun AppNavigation(appRepository: AppRepository) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            // Only show bottom bar on top level screens
            if (currentRoute in listOf(
                    BottomNavItem.Main.route, 
                    BottomNavItem.Work.route, 
                    BottomNavItem.Private.route, 
                    BottomNavItem.System.route,
                    BottomNavItem.Settings.route
                )) {
                NavigationBar {
                    val items = listOf(
                        BottomNavItem.Main,
                        BottomNavItem.Work,
                        BottomNavItem.Private,
                        BottomNavItem.System,
                        BottomNavItem.Settings
                    )
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController, 
            startDestination = BottomNavItem.Main.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavItem.Main.route) {
                AppListScreen(appRepository, com.appops.androidx.repository.AppUserType.PERSONAL, navController)
            }
            composable(BottomNavItem.Work.route) {
                AppListScreen(appRepository, com.appops.androidx.repository.AppUserType.WORK, navController)
            }
            composable(BottomNavItem.Private.route) {
                AppListScreen(appRepository, com.appops.androidx.repository.AppUserType.PRIVATE, navController)
            }
            composable(BottomNavItem.System.route) {
                AppListScreen(appRepository, com.appops.androidx.repository.AppUserType.SYSTEM, navController)
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(navController)
            }
            composable("appOps/{pkg}/{userId}") { backStackEntry ->
                val pkg = backStackEntry.arguments?.getString("pkg") ?: ""
                val userId = backStackEntry.arguments?.getString("userId")?.toIntOrNull() ?: 0
                AppOpsScreen(pkg, userId, onBack = { navController.popBackStack() })
            }
            composable("codeEditor") {
                CodeEditorScreen(onBack = { navController.popBackStack() })
            }
            composable("terminal") {
                TerminalScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
