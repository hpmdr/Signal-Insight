package cn.debubu.signalinsight.ui.cellular

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.debubu.signalinsight.R
import cn.debubu.signalinsight.data.cellular.CellularData
import cn.debubu.signalinsight.data.cellular.CellularRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class CellularViewModel constructor(
    private val repository: CellularRepository,
    application: Application
) : AndroidViewModel(application) {
    private val TAG = "CellularNewViewModel"

    var activeSim by mutableIntStateOf(1)
        private set

    private val _sim1Data = mutableStateOf<CellularData?>(null)
    private val _sim2Data = mutableStateOf<CellularData?>(null)

    private val _currentSignalData = mutableStateOf(SignalData())
    val currentSignalData: State<SignalData> = _currentSignalData

    private val _neighborCells = mutableStateOf<List<NeighborCellTableModel>>(emptyList())
    val neighborCells: State<List<NeighborCellTableModel>> = _neighborCells

    private var dataCollectionJob: Job? = null

    // === 每张卡独立的数据快照（供 HorizontalPager 使用） ===
    val sim1SignalData: State<SignalData> = derivedStateOf {
        _sim1Data.value?.servingCell?.let { cell ->
            SignalData(
                dbm = cell.dbm,
                progress = ((cell.dbm + 120) / 60f).coerceIn(0f, 1f),
                operatorName = cell.operatorName,
                networkType = cell.networkType,
                rsrp = cell.rsrp, rsrq = cell.rsrq, sinr = cell.sinr, rssi = cell.rssi,
                pci = cell.pci, earfcn = cell.earfcn, band = cell.band, tac = cell.tac
            )
        } ?: SignalData()
    }

    val sim2SignalData: State<SignalData> = derivedStateOf {
        _sim2Data.value?.servingCell?.let { cell ->
            SignalData(
                dbm = cell.dbm,
                progress = ((cell.dbm + 120) / 60f).coerceIn(0f, 1f),
                operatorName = cell.operatorName,
                networkType = cell.networkType,
                rsrp = cell.rsrp, rsrq = cell.rsrq, sinr = cell.sinr, rssi = cell.rssi,
                pci = cell.pci, earfcn = cell.earfcn, band = cell.band, tac = cell.tac
            )
        } ?: SignalData()
    }

    val sim1NeighborCells: State<List<NeighborCellTableModel>> = derivedStateOf {
        _sim1Data.value?.neighborCells?.map { neighbor ->
            NeighborCellTableModel(
                pci = neighbor.pci, earfcn = neighbor.earfcn, band = neighbor.band,
                rsrp = neighbor.rsrp, rsrq = neighbor.rsrq, sinr = neighbor.sinr
            )
        }?.sortedByDescending { it.rsrp } ?: emptyList()
    }

    val sim2NeighborCells: State<List<NeighborCellTableModel>> = derivedStateOf {
        _sim2Data.value?.neighborCells?.map { neighbor ->
            NeighborCellTableModel(
                pci = neighbor.pci, earfcn = neighbor.earfcn, band = neighbor.band,
                rsrp = neighbor.rsrp, rsrq = neighbor.rsrq, sinr = neighbor.sinr
            )
        }?.sortedByDescending { it.rsrp } ?: emptyList()
    }

    init {
        startDataCollection()
    }

    fun restartDataCollection() {
        dataCollectionJob?.cancel()
        startDataCollection()
        Log.d(TAG, "数据收集已重新启动")
    }

    private fun startDataCollection() {
        dataCollectionJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getDualSimCellularDataFlow().collect { (sim1Data, sim2Data) ->
                _sim1Data.value = sim1Data
                _sim2Data.value = sim2Data

                updateCurrentSignalData()
                updateNeighborCells()

                Log.d(TAG,"SIM 1: ${sim1Data.servingCell?.operatorName}, 邻小区: ${sim1Data.neighborCells.size}")
                Log.d(TAG,"SIM 2: ${sim2Data.servingCell?.operatorName}, 邻小区: ${sim2Data.neighborCells.size}")
            }
        }
    }

    private fun updateCurrentSignalData() {
        val currentData = if (activeSim == 1) _sim1Data.value else _sim2Data.value
        val signalData = currentData?.servingCell?.let { cell ->
            SignalData(
                dbm = cell.dbm,
                progress = ((cell.dbm + 120) / 60f).coerceIn(0f, 1f),
                operatorName = cell.operatorName,
                networkType = cell.networkType,
                rsrp = cell.rsrp,
                rsrq = cell.rsrq,
                sinr = cell.sinr,
                rssi = cell.rssi,
                pci = cell.pci,
                earfcn = cell.earfcn,
                band = cell.band,
                tac = cell.tac
            )
        } ?: SignalData()

        _currentSignalData.value = signalData
    }

    private fun updateNeighborCells() {
        val currentData = if (activeSim == 1) _sim1Data.value else _sim2Data.value
        val neighborList = currentData?.neighborCells?.map { neighbor ->
            NeighborCellTableModel(
                pci = neighbor.pci,
                earfcn = neighbor.earfcn,
                band = neighbor.band,
                rsrp = neighbor.rsrp,
                rsrq = neighbor.rsrq,
                sinr = neighbor.sinr
            )
        }?.sortedByDescending { it.rsrp } ?: emptyList()

        _neighborCells.value = neighborList
    }

    fun switchSim(simId: Int): Boolean {
        val simData = if (simId == 1) _sim1Data.value else _sim2Data.value

        if (simData == null) {
            return false
        }

        activeSim = simId
        updateCurrentSignalData()
        updateNeighborCells()
        return true
    }

    fun getSimOperatorName(simId: Int): String {
        val app = getApplication<Application>()
        val noSim = app.getString(R.string.operator_no_sim)
        val simData = if (simId == 1) _sim1Data.value else _sim2Data.value
        val operatorName = simData?.servingCell?.operatorName ?: "No SIM"
        return if (operatorName == "No SIM") noSim else operatorName
    }

    fun isSimInserted(simId: Int): Boolean {
        val simData = if (simId == 1) _sim1Data.value else _sim2Data.value
        val operatorName = simData?.servingCell?.operatorName
        return operatorName != null && operatorName != "No SIM"
    }

    fun getSimOptions(): List<String> {
        val sim1Name = _sim1Data.value?.servingCell?.operatorName
        val sim2Name = _sim2Data.value?.servingCell?.operatorName
        return listOfNotNull(
            if (sim1Name != null && sim1Name != "No SIM") sim1Name else null,
            if (sim2Name != null && sim2Name != "No SIM") sim2Name else null
        )
    }
}

data class SignalData(
    val dbm: Int = -120,
    val progress: Float = 0f,
    val operatorName: String = "Unknown",
    val networkType: String = "Unknown",
    val rsrp: Int = -120,
    val rsrq: Int = -20,
    val sinr: Int = -20,
    val rssi: Int = -120,
    val pci: Int = 0,
    val earfcn: Int = 0,
    val band: String = "",
    val tac: Int = 0
)

data class NeighborCellTableModel(
    val pci: Int,
    val earfcn: Int,
    val band: String,
    val rsrp: Int,
    val rsrq: Int,
    val sinr: Int
)
