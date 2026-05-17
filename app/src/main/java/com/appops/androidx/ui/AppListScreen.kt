package com.appops.androidx.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.appops.androidx.repository.AppInfo
import com.appops.androidx.repository.AppRepository
import com.appops.androidx.repository.AppUserType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    appRepository: AppRepository,
    targetUserType: AppUserType,
    navController: NavController
) {
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        apps = appRepository.getApps()
        isLoading = false
    }

    val filteredApps = apps.filter { app ->
        val matchesSearch = app.appName.contains(searchQuery, ignoreCase = true) || 
                            app.packageName.contains(searchQuery, ignoreCase = true)
        
        val matchesType = if (targetUserType == AppUserType.SYSTEM) {
            app.isSystemApp
        } else {
            app.userType == targetUserType && (showSystemApps || !app.isSystemApp)
        }
        
        matchesSearch && matchesType
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(when(targetUserType) {
                        AppUserType.PERSONAL -> "Main Apps"
                        AppUserType.WORK -> "Work Space"
                        AppUserType.PRIVATE -> "Private Space"
                        AppUserType.SYSTEM -> "System Apps"
                        else -> "Apps"
                    }) },
                    actions = {
                        if (targetUserType != AppUserType.SYSTEM) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text("System", style = MaterialTheme.typography.labelMedium)
                                Switch(
                                    checked = showSystemApps,
                                    onCheckedChange = { showSystemApps = it },
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                )
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search applications...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("No apps found.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredApps) { app ->
                        AppListItem(app = app, onClick = { 
                            navController.navigate("appOps/${app.packageName}/${app.userId}") 
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun AppListItem(app: AppInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = app.appName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            if (app.isSystemApp) {
                Text(text = "System App", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
