package cn.debubu.signalinsight.ui.permission

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.debubu.signalinsight.R
import cn.debubu.signalinsight.data.permission.PermissionManager
import kotlinx.coroutines.launch

/**
 * 权限 ViewModel — 遵循 Android 官方权限请求最佳实践
 *
 * 权限状态流转：
 *   1. INITIAL (从未请求) → 显示"授权"按钮
 *   2. REQUESTED (已弹窗请求) → 等待用户响应
 *   3. DENIED_ONCE (被拒绝一次) → 显示"再次授权"按钮 + 理由说明
 *   4. PERMANENTLY_DENIED (永久拒绝) → 显示"前往设置"按钮
 *   5. GRANTED (已授权) → 绿色勾选状态
 */
class PermissionViewModel constructor(
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _permissionRequirements = mutableStateListOf<PermissionRequirement>()
    val permissionRequirements: SnapshotStateList<PermissionRequirement> = _permissionRequirements

    private val _allPermissionsGranted = mutableStateOf(false)
    val allPermissionsGranted: State<Boolean> = _allPermissionsGranted

    /** 是否有权限被永久拒绝（用户勾选了"不再询问"） */
    private val _hasPermanentlyDenied = mutableStateOf(false)
    val hasPermanentlyDenied: State<Boolean> = _hasPermanentlyDenied

    /** 是否正在请求权限（控制弹窗） */
    private val _isRequestingPermissions = mutableStateOf(false)
    val isRequestingPermissions: State<Boolean> = _isRequestingPermissions

    init {
        initializePermissionRequirements()
    }

    private final val TAG = "PermissionViewModel"

    private fun initializePermissionRequirements() {
        _permissionRequirements.clear()

        // Android 13+ 需要 READ_BASIC_PHONE_STATE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            _permissionRequirements.addAll(
                listOf(
                    PermissionRequirement(
                        permission = Manifest.permission.READ_BASIC_PHONE_STATE,
                        titleResId = R.string.perm_phone_state_title,
                        descriptionResId = R.string.perm_phone_state_desc,
                        icon = "phone_android"
                    ),
                    PermissionRequirement(
                        permission = Manifest.permission.READ_PHONE_STATE,
                        titleResId = R.string.perm_phone_full_title,
                        descriptionResId = R.string.perm_phone_full_desc,
                        icon = "phone_android"
                    )
                )
            )
        } else {
            _permissionRequirements.add(
                PermissionRequirement(
                    permission = Manifest.permission.READ_PHONE_STATE,
                    titleResId = R.string.perm_phone_state_title,
                    descriptionResId = R.string.perm_phone_state_desc,
                    icon = "phone_android"
                )
            )
        }

        _permissionRequirements.add(
            PermissionRequirement(
                permission = Manifest.permission.ACCESS_FINE_LOCATION,
                titleResId = R.string.perm_location_title,
                descriptionResId = R.string.perm_location_desc,
                icon = "location_on"
            )
        )
    }

    /**
     * 检查所有权限状态 — 在页面首次加载、从后台返回、从设置页返回时调用
     */
    fun checkAllPermissions(activity: Activity?) {
        viewModelScope.launch {
            var allGranted = true
            var hasPermanentlyDenied = false

            _permissionRequirements.forEachIndexed { index, requirement ->
                val rawGranted = permissionManager.isPermissionGranted(requirement.permission)

                val isGranted = if (requirement.permission == Manifest.permission.ACCESS_FINE_LOCATION) {
                    rawGranted && permissionManager.isPreciseLocationEnabled()
                } else {
                    rawGranted
                }

                // 检测是否为"永久拒绝"状态
                // 只有在「曾经请求过」且「shouldShowRationale=false」时才是永久拒绝
                val isPermanentlyDenied = if (!isGranted && requirement.hasBeenRequested && activity != null) {
                    val canShowRationale = permissionManager.shouldShowRationale(activity, requirement.permission)
                    !canShowRationale
                } else {
                    false
                }

                if (!isGranted) {
                    allGranted = false
                }
                if (isPermanentlyDenied) {
                    hasPermanentlyDenied = true
                }

                // ★ 用 copy() 替换整个元素，确保 Compose Snapshot 系统能检测到变化
                _permissionRequirements[index] = requirement.copy(
                    isGranted = isGranted,
                    isPermanentlyDenied = isPermanentlyDenied
                )
            }

            _allPermissionsGranted.value = allGranted
            _hasPermanentlyDenied.value = hasPermanentlyDenied

            Log.d(TAG, "权限检查 - 全部授权: $allGranted, 永久拒绝: $hasPermanentlyDenied")
        }
    }

    /**
     * 请求权限 — 点击授权按钮时调用
     */
    fun requestPermissions(activity: Activity) {
        val permissionsToRequest = _permissionRequirements
            .filter { !it.isGranted && !it.isPermanentlyDenied }
            .map { it.permission }

        if (permissionsToRequest.isEmpty()) {
            Log.d(TAG, "没有可请求的权限")
            return
        }

        // 标记这些权限已经被请求过（用于后续判断永久拒绝）
        _permissionRequirements.forEach {
            if (it.permission in permissionsToRequest) {
                it.hasBeenRequested = true
            }
        }

        _isRequestingPermissions.value = true
    }

    /**
     * 处理权限请求结果 — 在 ActivityResultContracts.RequestMultiplePermissions 回调中调用
     */
    fun handlePermissionResult(
        permissions: List<String>,
        result: Map<String, Boolean>,
        activity: Activity?
    ) {
        viewModelScope.launch {
            _isRequestingPermissions.value = false

            _permissionRequirements.forEachIndexed { index, requirement ->
                if (requirement.permission in permissions) {
                    // 注意：已授权的权限不会出现在 dialog 中，result[perm] 为 null
                    val granted = if (result.containsKey(requirement.permission)) {
                        result[requirement.permission] == true
                    } else {
                        permissionManager.isPermissionGranted(requirement.permission)
                    }

                    val isGranted = if (requirement.permission == Manifest.permission.ACCESS_FINE_LOCATION) {
                        granted && permissionManager.isPreciseLocationEnabled()
                    } else {
                        granted
                    }

                    // 关键检测：判断是否为永久拒绝
                    val isPermanentlyDenied = if (!isGranted && requirement.hasBeenRequested && activity != null) {
                        val canShowRationale = permissionManager.shouldShowRationale(activity, requirement.permission)
                        !canShowRationale
                    } else {
                        false
                    }

                    // ★ 用 copy() 替换整个元素，确保 Compose Snapshot 系统检测到变化
                    _permissionRequirements[index] = requirement.copy(
                        isGranted = isGranted,
                        isPermanentlyDenied = isPermanentlyDenied
                    )
                }
            }

            // 重新汇总全局状态
            val allGranted = _permissionRequirements.all { it.isGranted }
            val hasPermanentlyDenied = _permissionRequirements.any { it.isPermanentlyDenied }
            _allPermissionsGranted.value = allGranted
            _hasPermanentlyDenied.value = hasPermanentlyDenied

            Log.d(TAG, "权限结果 - 全部授权: $allGranted, 永久拒绝: $hasPermanentlyDenied")
        }
    }

    /**
     * 跳转到系统设置页
     */
    fun navigateToSettings(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            activity.startActivity(intent)
            Log.d(TAG, "跳转到应用设置页面")
        } catch (e: Exception) {
            Log.e(TAG, "跳转设置页失败: ${e.message}")
        }
    }
}

/**
 * 单个权限的需求定义
 */
data class PermissionRequirement(
    val permission: String,
    val titleResId: Int,
    val descriptionResId: Int,
    val icon: String,
    var isGranted: Boolean = false,
    var isPermanentlyDenied: Boolean = false,
    /** 标记该权限是否曾经被请求过（用于区分"从未请求"和"永久拒绝"） */
    var hasBeenRequested: Boolean = false
) {
    val statusTextResId: Int
        get() = when {
            isGranted -> R.string.perm_status_granted
            isPermanentlyDenied -> R.string.perm_status_manual
            else -> R.string.perm_status_pending
        }
}
