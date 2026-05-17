package com.appops.androidx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.appops.androidx.privilege.PrivilegeManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(onBack: () -> Unit) {
    var commandInput by remember { mutableStateOf("") }
    var terminalOutput by remember { mutableStateOf<List<String>>(emptyList()) }
    var isExecuting by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(terminalOutput.size) {
        if (terminalOutput.isNotEmpty()) {
            listState.animateScrollToItem(terminalOutput.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminal") },
                navigationIcon = {
                    Button(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(8.dp)
            ) {
                items(terminalOutput) { line ->
                    Text(
                        text = line,
                        color = Color.Green,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter command...") },
                        singleLine = true,
                        enabled = !isExecuting,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (commandInput.isNotBlank() && !isExecuting) {
                                    val cmd = commandInput
                                    commandInput = ""
                                    terminalOutput = terminalOutput + "> $cmd"
                                    isExecuting = true
                                    coroutineScope.launch {
                                        val result = PrivilegeManager.runCommand(cmd)
                                        if (result.output.isNotBlank()) {
                                            terminalOutput = terminalOutput + result.output.lines()
                                        }
                                        if (result.error.isNotBlank()) {
                                            terminalOutput = terminalOutput + result.error.lines()
                                        }
                                        if (result.output.isBlank() && result.error.isBlank()) {
                                            terminalOutput = terminalOutput + "[Executed with no output]"
                                        }
                                        isExecuting = false
                                    }
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (commandInput.isNotBlank() && !isExecuting) {
                                val cmd = commandInput
                                commandInput = ""
                                terminalOutput = terminalOutput + "> $cmd"
                                isExecuting = true
                                coroutineScope.launch {
                                    val result = PrivilegeManager.runCommand(cmd)
                                    if (result.output.isNotBlank()) {
                                        terminalOutput = terminalOutput + result.output.lines()
                                    }
                                    if (result.error.isNotBlank()) {
                                        terminalOutput = terminalOutput + result.error.lines()
                                    }
                                    if (result.output.isBlank() && result.error.isBlank()) {
                                        terminalOutput = terminalOutput + "[Executed with no output]"
                                    }
                                    isExecuting = false
                                }
                            }
                        },
                        enabled = commandInput.isNotBlank() && !isExecuting
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send Command", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
