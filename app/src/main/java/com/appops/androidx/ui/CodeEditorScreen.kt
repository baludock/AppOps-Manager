package com.appops.androidx.ui

import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.appops.androidx.privilege.PrivilegeManager
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var targetFile by remember { mutableStateOf("/system/build.prop") }
    var fileContent by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var editorInstance by remember { mutableStateOf<CodeEditor?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Code Editor") },
                navigationIcon = {
                    Button(onClick = onBack) { Text("Back") }
                },
                actions = {
                    Button(onClick = {
                        coroutineScope.launch {
                            val result = PrivilegeManager.runCommand("cat $targetFile")
                            if (result.success) {
                                fileContent = result.output
                                editorInstance?.setText(fileContent)
                                statusMessage = "Loaded $targetFile"
                            } else {
                                statusMessage = "Error: ${result.error}"
                            }
                        }
                    }) {
                        Text("Load")
                    }
                    Button(onClick = {
                        coroutineScope.launch {
                            val content = editorInstance?.text.toString()
                            // Note: Writing to /system requires remounting rw, which is complex and often impossible on modern Android without Magisk modules.
                            // For /data files or other locations, root might be sufficient.
                            // Here we use a standard echo approach (might fail on large files due to quoting, a temp file + cp is better in production).
                            val tempFile = "/data/local/tmp/edit_temp"
                            val writeTemp = PrivilegeManager.runCommand("cat << 'EOF' > $tempFile\n$content\nEOF")
                            if (writeTemp.success) {
                                val moveFile = PrivilegeManager.runCommand("cp $tempFile $targetFile")
                                if (moveFile.success) {
                                    statusMessage = "Saved to $targetFile"
                                } else {
                                    statusMessage = "Error saving: ${moveFile.error}"
                                }
                            } else {
                                statusMessage = "Error writing temp: ${writeTemp.error}"
                            }
                        }
                    }) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = targetFile,
                onValueChange = { targetFile = it },
                label = { Text("File Path") },
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
            
            Text(text = statusMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))

            AndroidView(
                factory = { ctx ->
                    CodeEditor(ctx).apply {
                        // Basic setup for Sora Editor
                        isWordwrap = false
                        editorInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
