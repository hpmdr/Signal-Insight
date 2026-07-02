package cn.debubu.signalinsight.ui.cellular

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.debubu.signalinsight.R
import cn.debubu.signalinsight.data.cellular.CellularData
import cn.debubu.signalinsight.data.cellular.CellularRepository
import cn.debubu.signalinsight.data.cellular.CellularSignalInfo
import cn.debubu.signalinsight.data.cellular.NeighborCellTableModel
import cn.debubu.signalinsight.data.cellular.SignalData
import cn.debubu.signalinsight.data.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 蜂窝信号 ViewModel — 使用 StateFlow 实现 MVVM 架构
 *
 * 数据流：
 *   CellularRepository (callbackFlow)
 *     → _sim1Data / _sim2Data (MutableStateFlow)
 *       → sim1SignalData / sim2SignalData (derived map)
 *       → currentSignalData (combine)
 *     → Compose UI (collectAsState)
 */
class CellularViewModel(
    private val repository: CellularRepository,
    private val themeManager: ThemeManager,
    application: Application
) : AndroidViewModel(application) {

    private val TAG = "CellularViewModel"

    // ─── 原始数据源 ─────────────────────────────────────────────

    private val _sim1Data = MutableStateFlow<CellularData?>(null)
    private val _sim2Data = MutableStateFlow<CellularData?>(null)

    /** 当前选中的 SIM 卡槽 (1 或 2) */
    private val _activeSim = MutableStateFlow(1)
    val activeSim: StateFlow<Int> = _activeSim.asStateFlow()

    private var dataCollectionJob: Job? = null
    private var refreshJob: Job? = null
    private var settingsJob: Job? = null

    /** 当前刷新间隔（毫秒），由 ThemeManager 驱动 */
    private var refreshIntervalMs: Long = themeManager.settings.value.refreshIntervalMs.toLong()

    // ─── 派生状态 (每张卡独立) ─────────────────────────────────────

    /** SIM 1 的 UI 信号数据 */
    val sim1SignalData: StateFlow<SignalData> = _sim1Data
        .map { it?.servingCell.toSignalData() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SignalData())

    /** SIM 2 的 UI 信号数据 */
    val sim2SignalData: StateFlow<SignalData> = _sim2Data
        .map { it?.servingCell.toSignalData() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SignalData())

    /** SIM 1 的邻小区列表 */
    val sim1NeighborCells: StateFlow<List<NeighborCellTableModel>> = _sim1Data
        .map { data -> data?.neighborCells.toNeighborModels() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** SIM 2 的邻小区列表 */
    val sim2NeighborCells: StateFlow<List<NeighborCellTableModel>> = _sim2Data
        .map { data -> data?.neighborCells.toNeighborModels() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── 派生状态 (当前激活的卡) ────────────────────────────────────

    /** 当前激活 SIM 的 UI 信号数据 */
    val currentSignalData: StateFlow<SignalData> = combine(
        _activeSim, _sim1Data, _sim2Data
    ) { active, sim1, sim2 ->
        val data = if (active == 1) sim1 else sim2
        data?.servingCell.toSignalData()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SignalData())

    /** 当前激活 SIM 的邻小区列表 */
    val neighborCells: StateFlow<List<NeighborCellTableModel>> = combine(
        _activeSim, _sim1Data, _sim2Data
    ) { active, sim1, sim2 ->
        val data = if (active == 1) sim1 else sim2
        data?.neighborCells.toNeighborModels()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── 初始化 ─────────────────────────────────────────────────

    init {
        startDataCollection()
    }

    fun restartDataCollection() {
        dataCollectionJob?.cancel()
        refreshJob?.cancel()
        settingsJob?.cancel()
        startDataCollection()
        Log.d(TAG, "数据收集已重新启动")
    }

    /** 暂停数据收集（app 进入后台时调用，节省电量） */
    fun pauseDataCollection() {
        dataCollectionJob?.cancel()
        dataCollectionJob = null
        refreshJob?.cancel()
        refreshJob = null
        settingsJob?.cancel()
        settingsJob = null
        Log.d(TAG, "数据收集已暂停 - app 进入后台")
    }

    /** 恢复数据收集（app 回到前台时调用） */
    fun resumeDataCollection() {
        if (dataCollectionJob == null || dataCollectionJob?.isActive != true) {
            startDataCollection()
            Log.d(TAG, "数据收集已恢复 - app 回到前台")
        }
    }

    private fun startDataCollection() {
        dataCollectionJob = viewModelScope.launch(Dispatchers.IO) {
            var firstValidDataArrived = false
            repository.getDualSimCellularDataFlow().collect { (sim1Data, sim2Data) ->
                val sim1Valid = isSimValid(sim1Data)
                val sim2Valid = isSimValid(sim2Data)

                // ── 防抖保护 ──
                // 网络切换（5G↔4G）时系统会短暂发送空 CellInfo，导致 UI 闪烁 N/A。
                // 只有在「数据有效」或「从未有过有效数据」时才更新状态。
                val sim1HasValidEver = _sim1Data.value?.let { isSimValid(it) } ?: false
                val sim2HasValidEver = _sim2Data.value?.let { isSimValid(it) } ?: false

                if (sim1Valid || !sim1HasValidEver) {
                    _sim1Data.value = sim1Data
                }
                if (sim2Valid || !sim2HasValidEver) {
                    _sim2Data.value = sim2Data
                }

                Log.d(TAG, "SIM 1: ${sim1Data.servingCell?.operatorName} (valid=$sim1Valid)")
                Log.d(TAG, "SIM 2: ${sim2Data.servingCell?.operatorName} (valid=$sim2Valid)")

                // 首次有效数据到达时自动选择默认卡槽
                if (!firstValidDataArrived && (sim1Valid || sim2Valid)) {
                    firstValidDataArrived = true
                    selectDefaultSim(sim1Data, sim2Data)
                } else if (firstValidDataArrived) {
                    autoSwitchIfNeeded(sim1Data, sim2Data)
                }
            }
        }
        startPeriodicRefresh()
    }

    /** 前台周期性调用 requestCellInfoUpdate，刷新间隔由 ThemeManager 设置动态控制 */
    private fun startPeriodicRefresh() {
        refreshJob?.cancel()
        settingsJob?.cancel()

        // 监听 ThemeManager 中刷新间隔的变化
        settingsJob = viewModelScope.launch(Dispatchers.IO) {
            themeManager.settings.collect { settings ->
                val newInterval = settings.refreshIntervalMs.toLong()
                if (newInterval != refreshIntervalMs) {
                    refreshIntervalMs = newInterval
                    Log.d(TAG, "刷新间隔已更新: ${refreshIntervalMs}ms")
                }
            }
        }

        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                kotlinx.coroutines.delay(refreshIntervalMs)
                repository.requestCellInfoUpdate(0)
                repository.requestCellInfoUpdate(1)
            }
        }
    }

    /**
     * 根据插卡状态自动选择默认显示的 SIM 卡槽
     * - 仅卡 1 插卡 → 显示卡 1
     * - 仅卡 2 插卡 → 显示卡 2
     * - 双卡均已插 → 保持默认 1
     * - 均未插卡   → 保持默认 1
     */
    private fun selectDefaultSim(sim1Data: CellularData?, sim2Data: CellularData?) {
        val sim1Valid = isSimValid(sim1Data)
        val sim2Valid = isSimValid(sim2Data)

        _activeSim.value = when {
            !sim1Valid && sim2Valid -> 2
            else -> 1
        }
        Log.d(TAG, "自动选择卡槽 ${_activeSim.value} (SIM1=$sim1Valid, SIM2=$sim2Valid)")
    }

    /**
     * 运行时检测：如果当前选中的卡失去信号，且另一张卡有效，自动切换
     */
    private fun autoSwitchIfNeeded(sim1Data: CellularData?, sim2Data: CellularData?) {
        val sim1Valid = isSimValid(sim1Data)
        val sim2Valid = isSimValid(sim2Data)
        val current = _activeSim.value

        if (current == 1 && !sim1Valid && sim2Valid) {
            _activeSim.value = 2
            Log.d(TAG, "卡 1 失去信号，自动切换到卡 2")
        } else if (current == 2 && !sim2Valid && sim1Valid) {
            _activeSim.value = 1
            Log.d(TAG, "卡 2 失去信号，自动切换到卡 1")
        }
    }

    /** 判断指定卡槽的 CellularData 是否有效（有实际信号且运营商可识别） */
    private fun isSimValid(data: CellularData?): Boolean {
        val name = data?.servingCell?.operatorName
        return name != null && name != "未插卡" && name != "No SIM" && name != "Unknown"
    }

    // ─── 卡槽切换 ────────────────────────────────────────────────

    /**
     * 切换到指定的 SIM 卡槽
     * @return 切换是否成功（目标卡槽有数据）
     */
    fun switchSim(simId: Int): Boolean {
        val simData = if (simId == 1) _sim1Data.value else _sim2Data.value
        if (simData == null) return false

        _activeSim.value = simId
        return true
    }

    // ─── SIM 状态查询 ────────────────────────────────────────────

    /** 获取指定 SIM 卡槽的运营商名称（国际化字符串） */
    fun getSimOperatorName(simId: Int): String {
        val app = getApplication<Application>()
        val noSim = app.getString(R.string.operator_no_sim)
        val simData = if (simId == 1) _sim1Data.value else _sim2Data.value
        val operatorName = simData?.servingCell?.operatorName ?: "No SIM"
        return if (operatorName == "No SIM") noSim else operatorName
    }

    /** 判断指定 SIM 卡槽是否已插卡 */
    fun isSimInserted(simId: Int): Boolean {
        val simData = if (simId == 1) _sim1Data.value else _sim2Data.value
        val operatorName = simData?.servingCell?.operatorName
        return operatorName != null && operatorName != "未插卡" && operatorName != "No SIM"
    }

    /** 获取已插入的 SIM 卡运营商名称列表 */
    fun getSimOptions(): List<String> {
        val sim1Name = _sim1Data.value?.servingCell?.operatorName
        val sim2Name = _sim2Data.value?.servingCell?.operatorName
        fun isValid(name: String?) = name != null && name != "未插卡" && name != "No SIM" && name != "Unknown"
        return listOfNotNull(
            if (isValid(sim1Name)) sim1Name else null,
            if (isValid(sim2Name)) sim2Name else null
        )
    }
}

// ─── 私有扩展函数 ──────────────────────────────────────────────────

/** 将 [CellularSignalInfo] 转换为 UI 用的 [SignalData] */
private fun CellularSignalInfo?.toSignalData(): SignalData = this?.let { cell ->
    SignalData(
        dbm = cell.dbm,
        progress = if (cell.dbm != Int.MAX_VALUE) {
            ((cell.dbm + 120) / 60f).coerceIn(0f, 1f)
        } else 0f,
        operatorName = cell.operatorName,
        networkType = cell.networkType,
        rsrp = cell.rsrp,
        rsrq = cell.rsrq,
        sinr = cell.sinr,
        rssi = cell.rssi,
        pci = cell.pci,
        earfcn = cell.earfcn,
        band = cell.band,
        tac = cell.tac,
    )
} ?: SignalData()

/** 将邻小区列表转换为表格模型 */
private fun List<cn.debubu.signalinsight.data.cellular.NeighborCellInfo>?.toNeighborModels(): List<NeighborCellTableModel> =
    this?.map { neighbor ->
        NeighborCellTableModel(
            pci = neighbor.pci,
            earfcn = neighbor.earfcn,
            band = neighbor.band,
            rsrp = neighbor.rsrp,
            rsrq = neighbor.rsrq,
            sinr = neighbor.sinr,
        )
    }?.sortedByDescending { it.rsrp } ?: emptyList()
