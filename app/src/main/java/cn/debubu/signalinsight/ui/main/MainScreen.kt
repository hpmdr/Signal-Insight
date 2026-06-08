package cn.debubu.signalinsight.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.debubu.signalinsight.R
import cn.debubu.signalinsight.ui.cellular.CellularPage
import cn.debubu.signalinsight.ui.cellular.CellularViewModel
import cn.debubu.signalinsight.ui.permission.PermissionScreen
import cn.debubu.signalinsight.ui.permission.PermissionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    cellularViewModel: CellularViewModel,
    permissionViewModel: PermissionViewModel
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedDestination by remember { mutableStateOf(Destination.Cellular) }

    val allPermissionsGranted by permissionViewModel.allPermissionsGranted
    var isCheckingPermissions by remember { mutableStateOf(true) }
    var previousPermissionsGranted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        permissionViewModel.checkAllPermissions(context as? android.app.Activity)
        isCheckingPermissions = false
    }

    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted && !previousPermissionsGranted) {
            cellularViewModel.restartDataCollection()
        }
        previousPermissionsGranted = allPermissionsGranted
    }

    val destinations = listOf(
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

    ModalNavigationDrawer(
        drawerState = drawerState,
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
                                scope.launch {
                                    drawerState.close()
                                }
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_bar_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        androidx.compose.material3.IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.open_menu)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isCheckingPermissions) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
                    )
                } else if (allPermissionsGranted) {
                    when (selectedDestination) {
                        Destination.Cellular -> CellularPage(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = cellularViewModel
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
