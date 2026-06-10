package cn.debubu.signalinsight

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import cn.debubu.signalinsight.ui.cellular.CellularViewModel
import cn.debubu.signalinsight.ui.main.MainScreen
import cn.debubu.signalinsight.ui.permission.PermissionViewModel
import cn.debubu.signalinsight.ui.theme.SignalInsightTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val application = application as SignalInsightApplication
        val cellularViewModel = ViewModelProvider(
            this,
            CellularViewModelFactory(application.cellularRepository, application)
        )[CellularViewModel::class.java]

        val permissionViewModel = ViewModelProvider(
            this,
            PermissionViewModelFactory(application.permissionManager)
        )[PermissionViewModel::class.java]

        setContent {
            SignalInsightTheme {
                MainScreen(
                    cellularViewModel = cellularViewModel,
                    permissionViewModel = permissionViewModel
                )
            }
        }
    }
}

class CellularViewModelFactory(
    private val repository: cn.debubu.signalinsight.data.cellular.CellularRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CellularViewModel::class.java)) {
            return CellularViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class PermissionViewModelFactory(
    private val permissionManager: cn.debubu.signalinsight.data.permission.PermissionManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PermissionViewModel::class.java)) {
            return PermissionViewModel(permissionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}