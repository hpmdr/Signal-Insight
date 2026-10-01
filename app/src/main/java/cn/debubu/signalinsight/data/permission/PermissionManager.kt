package cn.debubu.signalinsight.data.permission

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

data class PermissionState(
    val allGranted: Boolean = false,
    val missingPermissions: List<String> = emptyList(),
    val permanentlyDenied: Boolean = false,
    val shouldRequest: Boolean = false
)

class PermissionManager constructor(private val context: Context) {

    /**
     * 检查权限状态 — 同时检测是否被永久拒绝（"不再询问"）
     *
     * 说明：`shouldShowRequestPermissionRationale()` 在「从未请求」和「永久拒绝」两种情况下
     * 都返回 false（官方 Activity 文档只说明它表示"是否应展示权限说明"），因此**单独使用它
     * 无法区分这两种状态**。本项目的做法是配合一个**持久化**的「是否请求过」标记来判断，
     * 详见 [markRequested] / [wasRequested]。
     */
    fun checkPermissions(permissions: List<String>, activity: Activity? = null): PermissionState {
        val missing = missingPermissions(permissions)
        val allGranted = missing.isEmpty()
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

    fun isPermissionGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

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

    /**
     * 「是否已请求过」标记的持久化。
     *
     * 该标记必须跨进程重启保留：`shouldShowRequestPermissionRationale()` 在「从未请求」与
     * 「永久拒绝」下都返回 false，若标记只存内存，则进程重启后会把「永久拒绝」误判为
     * 「从未请求」，导致用户点「授权」时系统不弹框、界面也不提示去设置页。
     *
     * 注意：读写发生在主线程（SharedPreferences 首次加载会阻塞），但文件极小、
     * 仅在权限页初始化和点击时访问，实测无感知。
     */
    private val prefs = context.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)

    fun markRequested(permission: String) {
        prefs.edit().putBoolean(permission, true).apply()
    }

    fun wasRequested(permission: String): Boolean = prefs.getBoolean(permission, false)
}
