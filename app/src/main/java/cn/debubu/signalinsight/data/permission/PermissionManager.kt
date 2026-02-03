package cn.debubu.signalinsight.data.permission

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

data class PermissionState(
    val allGranted: Boolean = false,
    val missingPermissions: List<String> = emptyList(),
    val permanentlyDenied: Boolean = false,
    val shouldRequest: Boolean = false
)

class PermissionManager constructor(private val context: Context) {

    fun checkPermissions(permissions: List<String>, activity: Activity? = null): PermissionState {
        val missing = missingPermissions(permissions)
        val allGranted = missing.isEmpty()
        val permanentlyDenied = false

        return PermissionState(
            allGranted = allGranted,
            missingPermissions = missing,
            permanentlyDenied = permanentlyDenied,
            shouldRequest = !permanentlyDenied && missing.isNotEmpty()
        )
    }

    fun missingPermissions(permissions: List<String>): List<String> =
        permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

    fun handlePermissionResult(
        permissions: List<String>,
        result: Map<String, Boolean>,
        activity: Activity?
    ): PermissionState {
        val allGranted = permissions.all { result[it] == true }
        val missing = missingPermissions(permissions)
        val permanentlyDenied = if (activity != null) {
            isPermanentlyDenied(activity, missing)
        } else {
            false
        }

        return PermissionState(
            allGranted = allGranted,
            missingPermissions = missing,
            permanentlyDenied = permanentlyDenied,
            shouldRequest = !permanentlyDenied && missing.isNotEmpty()
        )
    }

    fun isPermanentlyDenied(activity: Activity, permissions: List<String>): Boolean {
        if (permissions.isEmpty()) return false

        return permissions.any { perm ->
            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED &&
                    !shouldShowRationale(activity, perm)
        }
    }

    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return activity.shouldShowRequestPermissionRationale(permission)
    }

    fun isPreciseLocationEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return Settings.Secure.getInt(
                context.contentResolver,
                "location_accuracy",
                2
            ) == 2
        }
        return true
    }
}
