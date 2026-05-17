package com.appops.androidx.privilege

import android.content.pm.PackageManager
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

object PrivilegeManager {
    
    fun isRootAvailable(): Boolean {
        return Shell.isAppGrantedRoot() == true || Shell.getShell().isRoot
    }

    suspend fun requestPrivileges(): Boolean = withContext(Dispatchers.IO) {
        if (!Shell.getShell().isRoot) {
            Shell.cmd("su -c id").exec()
        }
        if (isRootAvailable()) return@withContext true
        
        if (isShizukuAvailable() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(1001)
        }
        return@withContext isRootAvailable() || (isShizukuAvailable() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
    }

    fun isShizukuAvailable(): Boolean {
        return Shizuku.pingBinder()
    }

    fun checkShizukuPermission(requestCode: Int): Boolean {
        if (Shizuku.isPreV11()) {
            return false
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            return true
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            return false
        } else {
            Shizuku.requestPermission(requestCode)
            return false
        }
    }

    suspend fun runCommand(command: String): CommandResult = withContext(Dispatchers.IO) {
        val hasRoot = isRootAvailable()
        val hasShizuku = isShizukuAvailable() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        
        if (hasRoot) {
            runViaRoot(command)
        } else if (hasShizuku) {
            runViaShizuku(command)
        } else {
            CommandResult(false, "", "No privileged access available")
        }
    }

    private fun runViaRoot(command: String): CommandResult {
        val result = Shell.cmd(command).exec()
        return CommandResult(
            success = result.isSuccess,
            output = result.out.joinToString("\n"),
            error = result.err.joinToString("\n")
        )
    }

    @Suppress("DEPRECATION")
    private fun runViaShizuku(command: String): CommandResult {
        try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val exitCode = process.waitFor()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            return CommandResult(exitCode == 0, output, error)
        } catch (e: Exception) {
            return CommandResult(false, "", e.message ?: "Unknown Shizuku error")
        }
    }
}

data class CommandResult(
    val success: Boolean,
    val output: String,
    val error: String
)
