package cn.debubu.signalinsight.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import cn.debubu.signalinsight.R
import cn.debubu.signalinsight.ui.cellular.CellularPage
import cn.debubu.signalinsight.ui.cellular.CellularViewModel
import cn.debubu.signalinsight.data.cellular.MetricKey
import cn.debubu.signalinsight.data.cellular.SignalData
import cn.debubu.signalinsight.ui.cellular.BandExplainer
import cn.debubu.signalinsight.ui.cellular.RsrpExplainer
import cn.debubu.signalinsight.ui.cellular.RsrqExplainer
import cn.debubu.signalinsight.ui.cellular.SinrExplainer
import cn.debubu.signalinsight.ui.cellular.RssiExplainer
import cn.debubu.signalinsight.ui.cellular.PciExplainer
import cn.debubu.signalinsight.ui.cellular.EarfcnExplainer
import cn.debubu.signalinsight.ui.cellular.TacExplainer
import cn.debubu.signalinsight.ui.permission.PermissionScreen
import cn.debubu.signalinsight.ui.permission.PermissionViewModel
import kotlinx.coroutines.launch

/**
 * 详情页路由状态：用 Pair 封装，避免 AnimatedContent 动画期间状态被清空导致崩溃
 * 当非 null 时显示详情页，为 null 时显示主页面
 */
data class ExplainerRoute(
    val key: MetricKey,
    val signalData: SignalData
)

data class NavigationItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val destination: Destination
)

enum class Destination {
    Cellular,
    About,
    Settings
}

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.about_title),
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.about_content),
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(
            modifier = Modifier.height(16.dp)
        )
        Text(
            text = stringResource(R.string.settings_placeholder),
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    cellularViewModel: CellularViewModel,
    permissionViewModel: PermissionViewModel
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 底部导航选中项
    var selectedDestination: Destination by remember { mutableStateOf(Destination.Cellular) }

    // ---- 详情页路由：用 ExplainerRoute? 封装，动画期间状态不会丢失 ----
    var explainerRoute by remember { mutableStateOf<ExplainerRoute?>(null) }

    // 权限状态
    val allPermissionsGranted by permissionViewModel.allPermissionsGranted
    var isCheckingPermissions by remember { mutableStateOf(true) }
    var previousPermissionsGranted by remember { mutableStateOf(false) }

    // 权限检查：首次进入 + 每次从后台返回（如从系统设置页授权后返回）都重新检测
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionViewModel.checkAllPermissions(context as? android.app.Activity)
                isCheckingPermissions = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted && !previousPermissionsGranted) {
            cellularViewModel.restartDataCollection()
        }
        previousPermissionsGranted = allPermissionsGranted
    }

    val destinations: List<NavigationItem> = listOf(
        NavigationItem(
            title = stringResource(R.string.menu_signal_monitor),
            icon = Icons.Default.Home,
            destination = Destination.Cellular
        ),
        NavigationItem(
            title = stringResource(R.string.menu_about),
            icon = Icons.Default.Info,
            destination = Destination.About
        ),
        NavigationItem(
            title = stringResource(R.string.menu_settings),
            icon = Icons.Default.Settings,
            destination = Destination.Settings
        )
    )

    // 详情页是否显示
    val isShowingExplainer = explainerRoute != null

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isShowingExplainer,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.menu_title),
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    destinations.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item.title) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            selected = selectedDestination == item.destination,
                            onClick = {
                                selectedDestination = item.destination
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    ) {
        // ─── 架构说明 ───
        // Scaffold 只有一层，始终存在
        // TopAppBar 根据是否显示详情页动态切换标题和导航图标
        // AnimatedContent 只包裹内容区域（Below TopAppBar），不包裹 Scaffold
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            // 详情页时显示 "XXX详解"，否则显示默认标题
                            text = if (isShowingExplainer) {
                                val route = explainerRoute!!
                                val titleResId = when (route.key) {
                                    MetricKey.Band -> R.string.metric_band_label
                                    MetricKey.RSRP -> R.string.metric_rsrp_label
                                    MetricKey.RSRQ -> R.string.metric_rsrq_label
                                    MetricKey.SINR -> R.string.metric_sinr_label
                                    MetricKey.RSSI -> R.string.metric_rssi_label
                                    MetricKey.PCI -> R.string.metric_pci_label
                                    MetricKey.EARFCN -> R.string.metric_earfcn_label
                                    MetricKey.TAC -> R.string.metric_tac_label
                                }
                                "${stringResource(titleResId)} ${stringResource(R.string.explainer_detail_title_suffix)}"
                            } else {
                                if (allPermissionsGranted) {
                                    stringResource(R.string.app_bar_title)
                                } else {
                                    stringResource(R.string.permission_title)
                                }
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (isShowingExplainer) {
                            // 详情页：显示返回箭头
                            androidx.compose.material3.IconButton(
                                onClick = { explainerRoute = null }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        } else {
                            // 主页面：显示菜单按钮
                            androidx.compose.material3.IconButton(
                                onClick = {
                                    scope.launch { drawerState.open() }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.open_menu)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { paddingValues ->
            // ---- 内容区域：AnimatedContent 只包裹这里 ----
            AnimatedContent(
                targetState = explainerRoute,
                transitionSpec = {
                    if (targetState != null) {
                        // 进入详情页：从右滑入 + 淡入，主页向左滑出
                        (slideInHorizontally { w -> w } + fadeIn(tween(300))) togetherWith
                            (slideOutHorizontally { w -> -w / 4 } + fadeOut(tween(250)))
                    } else {
                        // 返回主页：详情页向右滑出，主页从左侧滑入
                        (slideInHorizontally { w -> -w / 4 } + fadeIn(tween(300))) togetherWith
                            (slideOutHorizontally { w -> w } + fadeOut(tween(250)))
                    }
                },
                label = "mainPageTransition",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) { route ->
                if (route != null) {
                    // ★ 拦截系统返回键：回到主页面而非退出 App
                    BackHandler(onBack = { explainerRoute = null })

                    // ★ 详情页（无 Scaffold，TopAppBar 已由外层统一管理）
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp)
                    ) {
                        when (route.key) {
                            MetricKey.Band -> BandExplainer(currentBand = route.signalData.band, onClose = { explainerRoute = null })
                            MetricKey.RSRP -> RsrpExplainer(currentRsrp = route.signalData.dbm, onClose = { explainerRoute = null })
                            MetricKey.RSRQ -> RsrqExplainer(currentRsrq = route.signalData.rsrq, onClose = { explainerRoute = null })
                            MetricKey.SINR -> SinrExplainer(currentSinr = route.signalData.sinr, onClose = { explainerRoute = null })
                            MetricKey.RSSI -> RssiExplainer(currentRssi = route.signalData.rssi, onClose = { explainerRoute = null })
                            MetricKey.PCI -> PciExplainer(currentPci = route.signalData.pci, onClose = { explainerRoute = null })
                            MetricKey.EARFCN -> EarfcnExplainer(currentEarfcn = route.signalData.earfcn, onClose = { explainerRoute = null })
                            MetricKey.TAC -> TacExplainer(currentTac = route.signalData.tac, onClose = { explainerRoute = null })
                        }
                    }
                } else {
                    // ★ 拦截系统返回键：关于/设置页返回信号页，而非退出 App
                    if (selectedDestination != Destination.Cellular) {
                        BackHandler(onBack = { selectedDestination = Destination.Cellular })
                    }

                    // ★ 主页面（权限检查 → 信号页/关于/设置）
                    if (isCheckingPermissions) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp)
                        )
                    } else if (allPermissionsGranted) {
                        when (selectedDestination) {
                            Destination.Cellular -> CellularPage(
                                modifier = Modifier.fillMaxSize(),
                                viewModel = cellularViewModel,
                                onOpenExplainer = { key, signalData ->
                                    explainerRoute = ExplainerRoute(key, signalData)
                                }
                            )
                            Destination.About -> AboutScreen()
                            Destination.Settings -> SettingsScreen()
                        }
                    } else {
                        PermissionScreen(
                            onNavigateToMain = { },
                            viewModel = permissionViewModel
                        )
                    }
                }
            }
        }
    }
}
