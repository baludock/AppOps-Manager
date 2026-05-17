package com.appops.androidx.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.appops.androidx.privilege.PrivilegeManager
import com.appops.androidx.ui.theme.ThemeState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    var isRootAvailable by remember { mutableStateOf(PrivilegeManager.isRootAvailable()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("UI Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Dark Theme", style = MaterialTheme.typography.titleMedium)
                    Text("Enable futuristic material dark theme", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = ThemeState.isDarkThemeEnabled,
                    onCheckedChange = { ThemeState.isDarkThemeEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Privilege Management", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(32.dp))
            Text("Status", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Root Available: $isRootAvailable", style = MaterialTheme.typography.bodyLarge)
            Text("Shizuku Available: ${PrivilegeManager.isShizukuAvailable()}", style = MaterialTheme.typography.bodyLarge)
            
            val coroutineScope = rememberCoroutineScope()

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        PrivilegeManager.requestPrivileges()
                        isRootAvailable = PrivilegeManager.isRootAvailable()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Request Privileges (Root / Shizuku)")
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Advanced", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("codeEditor") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open System File Editor")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { navController.navigate("terminal") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Terminal")
            }
        }
    }
}
