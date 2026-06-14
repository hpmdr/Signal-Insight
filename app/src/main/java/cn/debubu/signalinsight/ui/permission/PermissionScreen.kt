package cn.debubu.signalinsight.ui.permission

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.debubu.signalinsight.R

@Composable
fun PermissionScreen(
    onNavigateToMain: () -> Unit,
    viewModel: PermissionViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val permissionRequirements = viewModel.permissionRequirements
    val allPermissionsGranted = viewModel.allPermissionsGranted.value
    val hasPermanentlyDenied = viewModel.hasPermanentlyDenied.value
    val isRequestingPermissions = viewModel.isRequestingPermissions.value

    // 首次进入时检查权限
    LaunchedEffect(Unit) {
        viewModel.checkAllPermissions(activity)
    }

    // 全部授权后自动跳转
    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted) {
            onNavigateToMain()
        }
    }

    // 需要请求的权限列表
    val permissionsToRequest = remember {
        permissionRequirements.map { it.permission }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        viewModel.handlePermissionResult(permissionsToRequest, result, activity)
    }

    // 当 ViewModel 设置 isRequestingPermissions 时弹框
    LaunchedEffect(isRequestingPermissions) {
        if (isRequestingPermissions) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp + 20.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp + 20.dp
        )
    ) {
        // 可滚动内容
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            HeaderSection()
            Spacer(modifier = Modifier.height(24.dp))

            PermissionCardsSection(
                permissionRequirements = permissionRequirements,
                onRequestPermission = { permission ->
                    // 单个权限卡点击 → 标记此权限，发起请求
                    viewModel.requestPermissions(activity ?: return@PermissionCardsSection)
                    // 注意：仍使用 launcher 弹完整权限框，由 ViewModel 控制
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 底部操作区
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 0.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 永久拒绝提示
                if (hasPermanentlyDenied) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error, null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                stringResource(R.string.permission_denied_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = {
                        if (hasPermanentlyDenied) {
                            viewModel.navigateToSettings(activity ?: return@Button)
                        } else {
                            viewModel.requestPermissions(activity ?: return@Button)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    enabled = !allPermissionsGranted
                ) {
                    Icon(
                        if (hasPermanentlyDenied) Icons.Default.Settings else Icons.Default.CheckCircle,
                        null, Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (hasPermanentlyDenied) stringResource(R.string.permission_go_settings)
                        else stringResource(R.string.permission_authorize),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PhoneAndroid, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.permission_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun PermissionCardsSection(
    permissionRequirements: List<PermissionRequirement>,
    onRequestPermission: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        permissionRequirements.forEach { req ->
            PermissionCard(req) { onRequestPermission(req.permission) }
        }
    }
}

@Composable
fun PermissionCard(
    requirement: PermissionRequirement,
    onRequestPermission: () -> Unit
) {
    val icon = when (requirement.icon) {
        "phone_android" -> Icons.Default.PhoneAndroid
        "location_on" -> Icons.Default.LocationOn
        else -> Icons.Default.PhoneAndroid
    }
    val statusColor = when {
        requirement.isGranted -> Color(0xFF386B28)
        requirement.isPermanentlyDenied -> Color(0xFFBA1A1A)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusIcon = when {
        requirement.isGranted -> Icons.Default.CheckCircle
        requirement.isPermanentlyDenied -> Icons.Default.Error
        else -> null
    }
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable(enabled = !requirement.isGranted && !requirement.isPermanentlyDenied) { onRequestPermission() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (requirement.isGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (!requirement.isGranted && !requirement.isPermanentlyDenied)
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Row(Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(Modifier.size(48.dp).clip(CircleShape)
                    .background(if (requirement.isGranted) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = if (requirement.isGranted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(requirement.titleResId), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(requirement.descriptionResId), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (statusIcon != null) {
                    Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(stringResource(requirement.statusTextResId), style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium, color = statusColor)
            }
        }
    }
}

