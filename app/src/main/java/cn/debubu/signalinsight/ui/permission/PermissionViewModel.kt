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
import cn.debubu.signalinsight.data.permission.PermissionManager
import kotlinx.coroutines.launch


class PermissionViewModel constructor(
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _permissionRequirements = mutableStateListOf<PermissionRequirement>()
    val permissionRequirements: SnapshotStateList<PermissionRequirement> = _permissionRequirements

    private val _allPermissionsGranted = mutableStateOf(false)
    val allPermissionsGranted: State<Boolean> = _allPermissionsGranted

    private val _shouldNavigateToSettings = mutableStateOf(false)
    val shouldNavigateToSettings: State<Boolean> = _shouldNavigateToSettings

    private val _isRequestingPermissions = mutableStateOf(false)
    val isRequestingPermissions: State<Boolean> = _isRequestingPermissions

    init {
        initializePermissionRequirements()
    }

    private final val TAG = "PermissionViewModel"

    private fun initializePermissionRequirements() {
        _permissionRequirements.clear()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            _permissionRequirements.addAll(
                listOf(
                    PermissionRequirement(
                        permission = Manifest.permission.READ_BASIC_PHONE_STATE,
                        title = "电话状态权限",
                        description = "读取基站与信号强度信息",
                        icon = "phone_android"
                    ),
                    PermissionRequirement(
                        permission = Manifest.permission.READ_PHONE_STATE,
                        title = "完整电话权限",
                        description = "获取双卡信息（Android 16+ 必需）",
                        icon = "phone_android"
                    )
                )
            )
        } else {
            _permissionRequirements.add(
                PermissionRequirement(
                    permission = Manifest.permission.READ_PHONE_STATE,
                    title = "电话状态权限",
                    description = "读取基站与信号强度信息",
                    icon = "phone_android"
                )
            )
        }

        _permissionRequirements.add(
            PermissionRequirement(
                permission = Manifest.permission.ACCESS_FINE_LOCATION,
                title = "精确位置权限",
                description = "关联基站地理位置（需选择'精确位置'）",
                icon = "location_on"
            )
        )
    }

    fun checkAllPermissions(activity: Activity?) {
        viewModelScope.launch {
            val permissions = _permissionRequirements.map { it.permission }
            val state = permissionManager.checkPermissions(permissions, activity)

            _permissionRequirements.forEach { requirement ->
                val isGranted = state.missingPermissions.contains(requirement.permission).not()

                if (requirement.permission == Manifest.permission.ACCESS_FINE_LOCATION) {
                    requirement.isGranted =
                        isGranted && permissionManager.isPreciseLocationEnabled()
                } else {
                    requirement.isGranted = isGranted
                }

                requirement.isPermanentlyDenied = false
            }

            _allPermissionsGranted.value = state.allGranted && isPreciseLocationGranted()
            _shouldNavigateToSettings.value = false

            Log.d(TAG, "权限检查完成 - 全部授权: ${_allPermissionsGranted.value}")
        }
    }

    fun handlePermissionResult(
        permissions: List<String>,
        result: Map<String, Boolean>,
        activity: Activity?
    ) {
        viewModelScope.launch {
            val state = permissionManager.handlePermissionResult(permissions, result, activity)

            _permissionRequirements.forEach { requirement ->
                val isGranted = result[requirement.permission] == true

                if (requirement.permission == Manifest.permission.ACCESS_FINE_LOCATION) {
                    requirement.isGranted =
                        isGranted && permissionManager.isPreciseLocationEnabled()
                } else {
                    requirement.isGranted = isGranted
                }

                requirement.isPermanentlyDenied = !requirement.isGranted &&
                        state.missingPermissions.contains(requirement.permission) &&
                        !permissionManager.shouldShowRationale(
                            activity ?: return@launch,
                            requirement.permission
                        )
            }

            _allPermissionsGranted.value = state.allGranted && isPreciseLocationGranted()
            _shouldNavigateToSettings.value = state.permanentlyDenied

            _isRequestingPermissions.value = false

            Log.d(
                TAG,
                "权限请求结果 - 全部授权: ${_allPermissionsGranted.value}, 永久拒绝: ${state.permanentlyDenied}"
            )
        }
    }

    fun requestPermissions() {
        _isRequestingPermissions.value = true
    }

    fun navigateToSettings(activity: Activity) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            activity.startActivity(intent)
            Log.d(TAG, "跳转到应用设置页面")
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    fun resetNavigationFlag() {
        _shouldNavigateToSettings.value = false
    }

    private fun isPreciseLocationGranted(): Boolean {
        val locationPermission = _permissionRequirements.find {
            it.permission == Manifest.permission.ACCESS_FINE_LOCATION
        }
        return locationPermission?.isGranted == true
    }
}

data class PermissionRequirement(
    val permission: String,
    val title: String,
    val description: String,
    val icon: String,
    var isGranted: Boolean = false,
    var isPermanentlyDenied: Boolean = false
) {
    val statusText: String
        get() = when {
            isGranted -> "已开启"
            isPermanentlyDenied -> "需手动开启"
            else -> "待授权"
        }
}
