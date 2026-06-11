package cn.debubu.signalinsight

import android.app.Application
import cn.debubu.signalinsight.data.cellular.CellularRepository
import cn.debubu.signalinsight.data.permission.PermissionManager
import cn.debubu.signalinsight.data.theme.ThemeManager

class SignalInsightApplication : Application() {

    val permissionManager by lazy {
        PermissionManager(applicationContext)
    }

    val cellularRepository by lazy {
        CellularRepository(applicationContext, permissionManager)
    }

    val themeManager by lazy {
        ThemeManager(applicationContext)
    }
}
