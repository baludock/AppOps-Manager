package com.appops.androidx.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appops.androidx.privilege.PrivilegeManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOpsScreen(packageName: String, userId: Int, onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var opsList by remember { mutableStateOf<List<AppOpItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isAppEnabled by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("") }
    
    var pendingChanges by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isApplying by remember { mutableStateOf(false) }

    LaunchedEffect(packageName, userId) {
        val result = PrivilegeManager.runCommand("cmd appops get --user $userId $packageName")
        if (result.success) {
            val parsed = result.output.lines().filter { it.isNotBlank() }.mapNotNull { line ->
                val parts = line.split(":")
                if (parts.size >= 2) {
                    val name = parts[0].trim()
                    val statePart = parts[1].trim().split(" ")[0].trim(';')
                    AppOpItem(name, statePart)
                } else null
            }
            opsList = parsed
        }
        
        // Fetch enabled state using shell for specific user
        val stateResult = PrivilegeManager.runCommand("pm list packages -e --user $userId | grep $packageName")
        if (stateResult.success && stateResult.output.contains(packageName)) {
            isAppEnabled = true
        } else {
            val disabledResult = PrivilegeManager.runCommand("pm list packages -d --user $userId | grep $packageName")
            if (disabledResult.success && disabledResult.output.contains(packageName)) {
                isAppEnabled = false
            } else {
                // Default to true if unsure, or try dumpsys
                isAppEnabled = true
            }
        }
        
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(packageName) },
                navigationIcon = {
                    Button(onClick = onBack) { Text("Back") }
                },
                actions = {}
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (pendingChanges.isNotEmpty()) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isApplying = true
                                var allSuccess = true
                                for ((opName, mode) in pendingChanges) {
                                    val res = PrivilegeManager.runCommand("cmd appops set --user $userId $packageName $opName $mode")
                                    if (!res.success) {
                                        allSuccess = false
                                    }
                                }
                                isApplying = false
                                if (allSuccess) {
                                    statusMessage = "Applied changes successfully."
                                    opsList = opsList.map { 
                                        if (pendingChanges.containsKey(it.name)) {
                                            it.copy(state = pendingChanges[it.name]!!)
                                        } else {
                                            it
                                        }
                                    }
                                    pendingChanges = emptyMap()
                                } else {
                                    statusMessage = "Failed to apply some changes."
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        enabled = !isApplying,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(if (isApplying) "Applying..." else "Apply Changes")
                    }
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("App Status: ${if (isAppEnabled) "Enabled" else "Disabled"}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val cmd = if (isAppEnabled) "pm disable-user --user $userId $packageName" else "pm enable --user $userId $packageName"
                                        val res = PrivilegeManager.runCommand(cmd)
                                        if (res.success) {
                                            isAppEnabled = !isAppEnabled
                                            statusMessage = "Successfully ${if (isAppEnabled) "enabled" else "disabled"} app."
                                        } else {
                                            statusMessage = "Failed: ${res.error}"
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAppEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isAppEnabled) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(if (isAppEnabled) "Disable App" else "Enable App")
                            }
                        }
                        if (statusMessage.isNotEmpty()) {
                            Text(statusMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
                
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(opsList) { op ->
                        val pendingState = pendingChanges[op.name]
                        OpListItem(
                            op = op,
                            pendingState = pendingState,
                            onStateChange = { newState ->
                                val updated = pendingChanges.toMutableMap()
                                if (newState == op.state) {
                                    updated.remove(op.name)
                                } else {
                                    updated[op.name] = newState
                                }
                                pendingChanges = updated
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OpListItem(op: AppOpItem, pendingState: String?, onStateChange: (String) -> Unit) {
    val displayState = pendingState ?: op.state

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = op.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "Current: ${op.state}${if (pendingState != null) " -> Pending: $pendingState" else ""}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("allow", "ignore", "deny", "default").forEach { mode ->
                    Button(
                        onClick = { onStateChange(mode) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (displayState == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f),
                            contentColor = if (displayState == mode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(mode, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

data class AppOpItem(val name: String, val state: String)
