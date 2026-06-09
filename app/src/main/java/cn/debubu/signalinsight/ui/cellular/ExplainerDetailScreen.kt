package cn.debubu.signalinsight.ui.cellular

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.debubu.signalinsight.R

/**
 * 全屏参数科普详情页 — 替代原来的 ModalBottomSheet
 *
 * 每个指标点击后跳转到此页面，包含完整的 TopAppBar + 返回按钮，
 * 内容完全可滚动，不受弹窗空间限制。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplainerDetailScreen(
    explainerKey: String,
    signalData: SignalData,
    onBack: () -> Unit
) {
    // 根据 key 确定标题
    val titleResId = when (explainerKey) {
        "Band" -> R.string.metric_band_label
        "RSRP" -> R.string.metric_rsrp_label
        "RSRQ" -> R.string.metric_rsrq_label
        "SINR" -> R.string.metric_sinr_label
        "RSSI" -> R.string.metric_rssi_label
        "PCI" -> R.string.metric_pci_label
        "EARFCN" -> R.string.metric_earfcn_label
        "TAC" -> R.string.metric_tac_label
        else -> R.string.app_bar_title
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${stringResource(titleResId)} ${stringResource(R.string.explainer_detail_title_suffix)}",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        // 拦截系统返回键：按返回时回到主页面
        BackHandler(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                // 注意：内部 Explainer 组件已有 verticalScroll，此处不可再添加
        ) {
            // 顶部留白，内容不紧贴标题栏
            Spacer(modifier = Modifier.height(16.dp))

            when (explainerKey) {
                "Band" -> BandExplainer(
                    currentBand = signalData.band,
                    onClose = onBack
                )
                "RSRP" -> RsrpExplainer(
                    currentRsrp = signalData.dbm,
                    onClose = onBack
                )
                "RSRQ" -> RsrqExplainer(
                    currentRsrq = signalData.rsrq,
                    onClose = onBack
                )
                "SINR" -> SinrExplainer(
                    currentSinr = signalData.sinr,
                    onClose = onBack
                )
                "RSSI" -> RssiExplainer(
                    currentRssi = signalData.rssi,
                    onClose = onBack
                )
                "PCI" -> PciExplainer(
                    currentPci = signalData.pci,
                    onClose = onBack
                )
                "EARFCN" -> EarfcnExplainer(
                    currentEarfcn = signalData.earfcn,
                    onClose = onBack
                )
                "TAC" -> TacExplainer(
                    currentTac = signalData.tac,
                    onClose = onBack
                )
            }
        }
    }
}
