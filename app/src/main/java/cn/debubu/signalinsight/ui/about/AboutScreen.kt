package cn.debubu.signalinsight.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.debubu.signalinsight.R

/**
 * 关于页面 — 版本号、隐私说明、开源信息。
 */
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "未知"
        } catch (_: Exception) {
            "未知"
        }
    }

    val gitHubUrl = "https://github.com/hpmdr/Signal-Insight"
    var showGitDialog by remember { mutableStateOf(false) }

    // ── 打开 GitHub 确认弹窗 ──
    if (showGitDialog) {
        AlertDialog(
            onDismissRequest = { showGitDialog = false },
            title = { Text(stringResource(R.string.about_view_source)) },
            text = { Text(stringResource(R.string.about_view_source_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showGitDialog = false
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(gitHubUrl))
                    context.startActivity(intent)
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showGitDialog = false }) { Text("取消") }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp + 20.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp + 20.dp,
                start = 16.dp, end = 16.dp
            )
    ) {
        Text(
            text = "信号监测仪 v$versionName",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "实时监测 2G/3G/4G/5G 蜂窝网络信号，了解你的网络环境。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── 隐私说明 ──
        Text(
            text = "隐私说明",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text("🔒", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "本应用不会联网",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "信号监测仪根本没有申请联网权限。你在系统设置中查看本应用的权限列表，找不到「网络」这一项——所有数据仅在本地处理，不可能上传或泄露到任何服务器。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                PermissionExplainRow(
                    title = "电话权限",
                    purpose = "读取基站信号强度、SIM 卡状态、网络类型等蜂窝信息，这是本应用的核心功能依赖。"
                )
                Spacer(Modifier.height(16.dp))
                PermissionExplainRow(
                    title = "位置权限",
                    purpose = "Android 系统要求：读取基站信息必须授予位置权限。本应用不会记录或追踪你的实际位置。"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 开源信息 ──
        Text(
            text = "开源协议",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "本项目采用 Apache 2.0 开源协议，源代码托管在 GitHub。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = gitHubUrl,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            lineHeight = 18.sp,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { showGitDialog = true }
        )
    }
}

@Composable
private fun PermissionExplainRow(title: String, purpose: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("📱", fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                purpose,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                lineHeight = 18.sp
            )
        }
    }
}
