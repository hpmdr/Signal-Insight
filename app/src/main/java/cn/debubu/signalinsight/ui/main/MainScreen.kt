package cn.debubu.signalinsight.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cn.debubu.signalinsight.R
import cn.debubu.signalinsight.data.cellular.MetricKey
import cn.debubu.signalinsight.data.cellular.SignalData
import cn.debubu.signalinsight.ui.cellular.BandExplainer
import cn.debubu.signalinsight.ui.cellular.CellularPage
import cn.debubu.signalinsight.ui.cellular.CellularViewModel
import cn.debubu.signalinsight.ui.cellular.EarfcnExplainer
import cn.debubu.signalinsight.ui.cellular.PciExplainer
import cn.debubu.signalinsight.ui.cellular.RsrpExplainer
import cn.debubu.signalinsight.ui.cellular.RsrqExplainer
import cn.debubu.signalinsight.ui.cellular.RssiExplainer
import cn.debubu.signalinsight.ui.cellular.SignalOverviewScreen
import cn.debubu.signalinsight.ui.cellular.SinrExplainer
import cn.debubu.signalinsight.ui.cellular.TacExplainer
import cn.debubu.signalinsight.ui.permission.PermissionScreen
import cn.debubu.signalinsight.ui.permission.PermissionViewModel
import cn.debubu.signalinsight.ui.settings.SettingsScreen
import cn.debubu.signalinsight.ui.settings.ThemeViewModel
import kotlinx.coroutines.launch

/**
 * 导航路由常量
 */
object NavRoutes {
    const val CELLULAR = "cellular"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val EXPLAINER = "explainer/{metricKey}"

    fun explainer(metricKey: MetricKey): String = "explainer/${metricKey.name}"
}

data class NavigationItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
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
            text = stringResource(R.string.about_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.about_content),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * 根据 MetricKey 渲染对应的详解页内容（不包含导航外壳）
 */
@Composable
fun ExplainerContent(metricKey: MetricKey, signalData: SignalData, onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        when (metricKey) {
            MetricKey.OVERVIEW -> SignalOverviewScreen(signalData = signalData, onClose = onClose)
            MetricKey.Band -> BandExplainer(currentBand = signalData.band, onClose = onClose)
            MetricKey.RSRP -> RsrpExplainer(currentRsrp = signalData.dbm, onClose = onClose)
            MetricKey.RSRQ -> RsrqExplainer(currentRsrq = signalData.rsrq, onClose = onClose)
            MetricKey.SINR -> SinrExplainer(currentSinr = signalData.sinr, onClose = onClose)
            MetricKey.RSSI -> RssiExplainer(currentRssi = signalData.rssi, onClose = onClose)
            MetricKey.PCI -> PciExplainer(currentPci = signalData.pci, onClose = onClose)
            MetricKey.EARFCN -> EarfcnExplainer(currentEarfcn = signalData.earfcn, onClose = onClose)
            MetricKey.TAC -> TacExplainer(currentTac = signalData.tac, onClose = onClose)
        }
    }
}

/**
 * 获取对应 MetricKey 的 TopAppBar 标题资源 ID
 */
private fun titleResIdForMetricKey(key: MetricKey): Int = when (key) {
    MetricKey.OVERVIEW -> R.string.overview_title
    MetricKey.Band -> R.string.metric_band_label
    MetricKey.RSRP -> R.string.metric_rsrp_label
    MetricKey.RSRQ -> R.string.metric_rsrq_label
    MetricKey.SINR -> R.string.metric_sinr_label
    MetricKey.RSSI -> R.string.metric_rssi_label
    MetricKey.PCI -> R.string.metric_pci_label
    MetricKey.EARFCN -> R.string.metric_earfcn_label
    MetricKey.TAC -> R.string.metric_tac_label
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    cellularViewModel: CellularViewModel,
    permissionViewModel: PermissionViewModel,
    themeViewModel: ThemeViewModel
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    // 70% 透明度 = 30% 不透明度的 bar 背景，形成毛玻璃效果
    val barScrim = Color.Black.copy(alpha = 0.45f)

    // 判断当前是否为详解页（用于控制抽屉手势 + TopAppBar 标题）
    val isShowingExplainer = currentRoute?.startsWith("explainer") == true

    // 侧栏打开时，返回键关闭侧栏
    if (drawerState.currentValue == DrawerValue.Open) {
        BackHandler(onBack = { scope.launch { drawerState.close() } })
    }

    // 权限状态
    val allPermissionsGranted by permissionViewModel.allPermissionsGranted
    var isCheckingPermissions by remember { mutableStateOf(true) }
    var previousPermissionsGranted by remember { mutableStateOf(false) }

    // 生命周期管理
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    permissionViewModel.checkAllPermissions(context as? android.app.Activity)
                    isCheckingPermissions = false
                    if (allPermissionsGranted) {
                        cellularViewModel.resumeDataCollection()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    cellularViewModel.pauseDataCollection()
                }
                else -> {}
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

    // 侧边菜单项
    val navItems = listOf(
        NavigationItem(
            title = stringResource(R.string.menu_signal_monitor),
            icon = Icons.Default.Home,
            route = NavRoutes.CELLULAR
        ),
        NavigationItem(
            title = stringResource(R.string.menu_settings),
            icon = Icons.Default.Settings,
            route = NavRoutes.SETTINGS
        ),
        NavigationItem(
            title = stringResource(R.string.menu_about),
            icon = Icons.Default.Info,
            route = NavRoutes.ABOUT
        )
    )

    // 当前详解页的 MetricKey（用于 TopAppBar 标题）
    val currentMetricKey = navBackStackEntry
        ?.arguments?.getString("metricKey")
        ?.let { try { MetricKey.valueOf(it) } catch (_: Exception) { null } }

    // 当前详解页标题
    val explainerTitle = currentMetricKey?.let { key ->
        val resId = titleResIdForMetricKey(key)
        if (key == MetricKey.OVERVIEW) stringResource(resId)
        else "${stringResource(resId)} ${stringResource(R.string.explainer_detail_title_suffix)}"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isShowingExplainer,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.menu_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    navItems.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item.title) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        // 避免在返回栈中重复添加同一目的地
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (isShowingExplainer) {
                                explainerTitle ?: ""
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
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.open_menu)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = barScrim,
                        scrolledContainerColor = barScrim,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = NavRoutes.CELLULAR,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // ── 主页面：信号监测 ──
                composable(
                    route = NavRoutes.CELLULAR,
                    enterTransition = { fadeIn(animationSpec = tween(250)) },
                    exitTransition = { fadeOut(animationSpec = tween(150)) }
                ) {
                    if (isCheckingPermissions) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp)
                        )
                    } else if (allPermissionsGranted) {
                        CellularPage(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = cellularViewModel,
                            onOpenExplainer = { key ->
                                navController.navigate(NavRoutes.explainer(key))
                            }
                        )
                    } else {
                        PermissionScreen(
                            onNavigateToMain = { },
                            viewModel = permissionViewModel
                        )
                    }
                }

                // ── 主页面：设置 ──
                composable(
                    route = NavRoutes.SETTINGS,
                    enterTransition = { fadeIn(animationSpec = tween(250)) },
                    exitTransition = { fadeOut(animationSpec = tween(150)) }
                ) {
                    BackHandler {
                        navController.popBackStack(NavRoutes.CELLULAR, inclusive = false)
                    }
                    SettingsScreen(viewModel = themeViewModel)
                }

                // ── 主页面：关于 ──
                composable(
                    route = NavRoutes.ABOUT,
                    enterTransition = { fadeIn(animationSpec = tween(250)) },
                    exitTransition = { fadeOut(animationSpec = tween(150)) }
                ) {
                    BackHandler {
                        navController.popBackStack(NavRoutes.CELLULAR, inclusive = false)
                    }
                    AboutScreen()
                }

                // ── 详解页 ──
                composable(
                    route = NavRoutes.EXPLAINER,
                    arguments = listOf(navArgument("metricKey") { type = NavType.StringType }),
                    enterTransition = {
                        slideInHorizontally(animationSpec = tween(300)) { w -> w } +
                                fadeIn(animationSpec = tween(250))
                    },
                    exitTransition = {
                        slideOutHorizontally(animationSpec = tween(250)) { w -> -w / 4 } +
                                fadeOut(animationSpec = tween(200))
                    },
                    popEnterTransition = {
                        slideInHorizontally(animationSpec = tween(300)) { w -> -w / 4 } +
                                fadeIn(animationSpec = tween(250))
                    },
                    popExitTransition = {
                        slideOutHorizontally(animationSpec = tween(250)) { w -> w } +
                                fadeOut(animationSpec = tween(200))
                    }
                ) { backStackEntry ->
                    val metricKeyStr = backStackEntry.arguments?.getString("metricKey") ?: "RSRP"
                    val metricKey = try {
                        MetricKey.valueOf(metricKeyStr)
                    } catch (_: Exception) {
                        MetricKey.RSRP
                    }
                    val signalData by cellularViewModel.currentSignalData.collectAsState()

                    BackHandler { navController.popBackStack() }

                    ExplainerContent(
                        metricKey = metricKey,
                        signalData = signalData,
                        onClose = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
