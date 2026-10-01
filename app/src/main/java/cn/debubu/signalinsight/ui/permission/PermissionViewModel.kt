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

    /** 上一次实际发起的请求集合 */
    private var _lastRequestedPermissions: List<String> = emptyList()

    init {
        initializePermissionRequirements()
        // 跨进程恢复「是否请求过」，用于区分「从未请求」与「永久拒绝」
        _permissionRequirements.forEach {
            it.hasBeenRequested = permissionManager.wasRequested(it.permission)
        }
    }

    private final val TAG = "PermissionViewModel"

    private fun initializePermissionRequirements() {
        _permissionRequirements.clear()

        // 注意：READ_BASIC_PHONE_STATE 是 **non-dangerous** 权限（安装即授予，不属运行时框架），
        // 因此不列入运行时请求清单；manifest 仍需保留声明，供 Android 13+ 的
        // getDataNetworkType() 使用（官方：READ_PHONE_STATE **或** READ_BASIC_PHONE_STATE）。
        // 把它放进请求数组是空操作，且一旦被误判为「永久拒绝」，会把用户送到一个
        // 根本没有该开关的系统设置页。
        _permissionRequirements.add(
            PermissionRequirement(
                permission = Manifest.permission.READ_PHONE_STATE,
                titleResId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    R.string.perm_phone_full_title
                } else {
                    R.string.perm_phone_state_title
                },
                descriptionResId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    R.string.perm_phone_full_desc
                } else {
                    R.string.perm_phone_state_desc
                },
                icon = "phone_android"
            )
        )

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

                // 精确位置由 ACCESS_FINE_LOCATION 的授予状态本身决定：用户在 Android 12+
                // 选择「大致位置」时，系统只授予 COARSE 而不授予 FINE。
                // （原先额外读 Settings.Secure 的未公开键 "location_accuracy"，该键在真机上
                //  并不存在，实测恒为默认值 2，属无效判断且存在误判风险，已移除。）
                val isGranted = rawGranted

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
        val permissionsToRequest = buildRequestList()

        if (permissionsToRequest.isEmpty()) {
            Log.d(TAG, "没有可请求的权限")
            return
        }

        _lastRequestedPermissions = permissionsToRequest

        // 标记这些权限已经被请求过（用于后续判断永久拒绝），并持久化
        _permissionRequirements.forEach {
            if (it.permission in permissionsToRequest) {
                it.hasBeenRequested = true
                permissionManager.markRequested(it.permission)
            }
        }

        _isRequestingPermissions.value = true
    }

    /** 上一次实际发起的请求集合（ActivityResult 回调须与之对齐） */
    fun lastRequestedPermissions(): List<String> = _lastRequestedPermissions

    /**
     * 供仪器测试校验请求集合构造规则（是否为 FINE 配对 COARSE、是否排除了非运行时权限）。
     * 生产代码路径与此完全相同，避免测试复制一份实现导致漂移。
     */
    @androidx.annotation.VisibleForTesting
    fun buildRequestListForTest(): List<String> = buildRequestList()

    /**
     * 构造实际向系统发起的请求集合。
     *
     * 官方要求：**不要单独请求 ACCESS_FINE_LOCATION**，必须与 ACCESS_COARSE_LOCATION 在同一次
     * 请求中一起提交；否则部分 Android 12 版本会忽略整个请求，并在 Logcat 打印
     * `ACCESS_FINE_LOCATION must be requested with ACCESS_COARSE_LOCATION`。
     */
    private fun buildRequestList(): List<String> {
        val base = _permissionRequirements
            .filter { !it.isGranted && !it.isPermanentlyDenied }
            .map { it.permission }
            .toMutableList()

        if (base.contains(Manifest.permission.ACCESS_FINE_LOCATION) &&
            !base.contains(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            base.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        return base
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

                    val isGranted = granted

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
