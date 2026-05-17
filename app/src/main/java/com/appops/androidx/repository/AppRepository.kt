package com.appops.androidx.repository

import android.content.Context
import android.content.pm.PackageManager
import com.appops.androidx.privilege.PrivilegeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val userType: AppUserType,
    val userId: Int
)

enum class AppUserType {
    PERSONAL,
    WORK,
    PRIVATE,
    SYSTEM,
    OTHER
}

class AppRepository(private val context: Context) {

    suspend fun getApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val apps = mutableListOf<AppInfo>()
        val pm = context.packageManager
        
        // 1. Get user mappings
        val users = mutableMapOf<Int, AppUserType>()
        users[0] = AppUserType.PERSONAL
        
        val usersResult = PrivilegeManager.runCommand("pm list users")
        if (usersResult.success) {
            // Parse UserInfo{id:name:flags}
            val regex = Regex("""UserInfo\{(\d+):([^:]+):([0-9a-fA-F]+)\}""")
            usersResult.output.lines().forEach { line ->
                val match = regex.find(line)
                if (match != null) {
                    val id = match.groupValues[1].toInt()
                    val name = match.groupValues[2].lowercase()
                    val typeFlags = match.groupValues[3] // We can use flags, but name matching is a good heuristic
                    
                    if (id != 0) {
                        if (name.contains("work") || name.contains("managed")) {
                            users[id] = AppUserType.WORK
                        } else if (name.contains("private") || typeFlags.contains("1000")) { // 1000 is often associated with private space flags, but name is safer
                            users[id] = AppUserType.PRIVATE
                        } else {
                            users[id] = AppUserType.OTHER
                        }
                    }
                }
            }
        }

        // 2. Fetch packages for each user
        users.forEach { (userId, userType) ->
            val pkgResult = PrivilegeManager.runCommand("pm list packages -s --user $userId")
            val systemPkgs = if (pkgResult.success) {
                pkgResult.output.lines().filter { it.startsWith("package:") }.map { it.removePrefix("package:") }.toSet()
            } else emptySet()

            val allPkgResult = PrivilegeManager.runCommand("pm list packages --user $userId")
            if (allPkgResult.success) {
                allPkgResult.output.lines().filter { it.startsWith("package:") }.forEach { line ->
                    val pkg = line.removePrefix("package:")
                    
                    // Attempt to get app name using standard PM (might fail for other users without INTERACT_ACROSS_USERS)
                    // If it fails, fallback to package name
                    var appName = pkg
                    try {
                        val info = pm.getApplicationInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
                        appName = info.loadLabel(pm).toString()
                    } catch (e: Exception) {}

                    apps.add(
                        AppInfo(
                            packageName = pkg,
                            appName = appName,
                            isSystemApp = systemPkgs.contains(pkg),
                            userType = userType,
                            userId = userId
                        )
                    )
                }
            }
        }

        apps.sortBy { it.appName.lowercase() }
        apps
    }
}
