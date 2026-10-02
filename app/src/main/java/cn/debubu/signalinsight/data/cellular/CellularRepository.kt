package cn.debubu.signalinsight.data.cellular

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellSignalStrengthLte
import android.telephony.SignalStrength
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import cn.debubu.signalinsight.data.permission.PermissionManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.concurrent.Executors

/**
 * 蜂窝信号数据仓库
 * 负责从 Android TelephonyManager API 获取蜂窝信号数据
 * 支持双卡和邻小区信息
 *
 * @param context 应用上下文
 */
class CellularRepository constructor(
    private val context: Context,
    private val permissionManager: PermissionManager
) {

    private final val TAG = "CellularRepository"

    private val telephonyManager: TelephonyManager by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    /**
     * 获取指定 SIM 卡槽的 TelephonyManager
     *
     * @param slotId SIM 卡槽 ID (0 或 1)
     * @return 对应的 TelephonyManager 实例，如果不可用则返回 null
     */
    private fun getTelephonyManagerForSlot(slotId: Int): TelephonyManager? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.createForSubscriptionId(getSubscriptionIdForSlot(slotId))
        } else {
            if (slotId == 0) telephonyManager else null
        }
    }

    /**
     * 获取指定卡槽的订阅 ID
     *
     * @param slotId SIM 卡槽 ID
     * @return 订阅 ID，如果不可用则返回默认值
     */
    private fun getSubscriptionIdForSlot(slotId: Int): Int {
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                as SubscriptionManager

        // 读取 activeSubscriptionInfoList 需要 READ_PHONE_STATE；权限缺失时会抛 SecurityException。
        // 官方 lint(MissingPermission)要求「显式检查权限或显式处理异常」，此处按后者处理：
        // 失败时回退到默认订阅 ID，由调用方按「未插卡」分支展示，绝不因此崩溃。
        val activeSubscriptionInfoList = try {
            subscriptionManager.activeSubscriptionInfoList
        } catch (e: SecurityException) {
            Log.w(TAG, "读取订阅列表被拒（缺少 READ_PHONE_STATE）", e)
            null
        }
        if (activeSubscriptionInfoList != null) {
            for (subscriptionInfo in activeSubscriptionInfoList) {
                if (subscriptionInfo.simSlotIndex == slotId) {
                    return subscriptionInfo.subscriptionId
                }
            }
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        } else {
            Int.MAX_VALUE
        }
    }

    /**
     * 获取指定卡槽的运营商名称
     *
     * @param slotId SIM 卡槽 ID
     * @return 运营商名称
     */
    private fun getOperatorNameForSlot(slotId: Int): String {
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                as SubscriptionManager

        // 同 getSubscriptionIdForSlot：显式处理 SecurityException（官方 lint 要求），
        // 失败时按「未插卡」返回，不向上抛。
        val activeSubscriptionInfoList = try {
            subscriptionManager.activeSubscriptionInfoList
        } catch (e: SecurityException) {
            Log.w(TAG, "读取订阅列表被拒（缺少 READ_PHONE_STATE）", e)
            null
        }
        if (activeSubscriptionInfoList != null) {
            for (subscriptionInfo in activeSubscriptionInfoList) {
                if (subscriptionInfo.simSlotIndex == slotId) {
                    val displayName = subscriptionInfo.displayName?.toString()
                    val carrierName = subscriptionInfo.carrierName?.toString()
                    val mcc = subscriptionInfo.mccString
                    val mnc = subscriptionInfo.mncString

                    Log.d(TAG, "SIM 卡槽: $slotId, displayName: $displayName, carrierName: $carrierName, mcc: $mcc, mnc: $mnc")

                    return displayName ?: carrierName ?: getOperatorNameByMccMnc(mcc, mnc)
                }
            }
        }

        return "未插卡"
    }

    private fun getNetworkType(slotId: Int): String {
        val tm = getTelephonyManagerForSlot(slotId) ?: return "未知"
        // dataNetworkType / networkType 需要 READ_PHONE_STATE 或 READ_BASIC_PHONE_STATE；
        // 权限缺失时抛 SecurityException，此处显式处理并降级为「未知」。
        val networkType = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                @Suppress("DEPRECATION")
                tm.dataNetworkType
            } else {
                @Suppress("DEPRECATION")
                tm.networkType
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "读取网络类型被拒（缺少电话权限）- SIM 卡槽: $slotId", e)
            TelephonyManager.NETWORK_TYPE_UNKNOWN
        }
        return CellularSignalInfo.getNetworkTypeName(networkType)
    }

    /**
     * 从 CellInfo 列表中提取服务小区和邻小区信息
     *
     * @param cellInfoList CellInfo 列表
     * @param slotId SIM 卡槽 ID
     * @param isPrimary 是否为主卡
     * @param operatorName 运营商名称（从 SubscriptionInfo 获取）
     * @return CellularData 对象
     */
    private fun extractCellularData(
        cellInfoList: List<CellInfo>,
        slotId: Int,
        isPrimary: Boolean,
        operatorName: String = "未知",
        lteSinrFallback: Int = Int.MAX_VALUE
    ): CellularData {
        if (cellInfoList.isEmpty()) {
            Log.w(TAG, "CellInfo 列表为空 - SIM 卡槽: $slotId, 是否为主卡: $isPrimary")
            return CellularData(
                servingCell = CellularSignalInfo(
                    simSlotId = slotId,
                    networkType = "未知",
                    isPrimary = isPrimary,
                    operatorName = operatorName
                )
            )
        }

        var servingCell: CellularSignalInfo? = null
        val neighborCells = mutableListOf<NeighborCellInfo>()

        for (cellInfo in cellInfoList) {
            if (cellInfo.isRegistered) {
                servingCell = CellularSignalInfo.fromCellInfo(cellInfo, slotId, isPrimary, operatorName, lteSinrFallback)
                Log.d(
                    TAG,
                    "服务小区信息 - SIM 卡槽: $slotId, PCI: ${servingCell.pci}, RSRP: ${servingCell.rsrp} dBm, EARFCN: ${servingCell.earfcn}, 运营商: ${servingCell.operatorName}, 频段: ${servingCell.band}, 网络类型: ${servingCell.networkType}"
                )
            } else {
                val neighborCell = NeighborCellInfo.fromCellInfo(cellInfo, isServing = false)
                neighborCells.add(neighborCell)
                Log.d(
                    TAG,
                    "邻小区信息 - SIM 卡槽: $slotId, PCI: ${neighborCell.pci}, RSRP: ${neighborCell.rsrp} dBm, SINR: ${neighborCell.sinr}, EARFCN: ${neighborCell.earfcn}, 频段: ${neighborCell.band}"
                )

            }
        }

        if (servingCell == null) {
            Log.w(
                TAG,
                "未找到服务小区 - SIM 卡槽: $slotId, CellInfo 总数: ${cellInfoList.size}, 邻小区数: ${neighborCells.size}, 判定为未插卡"
            )
            servingCell = CellularSignalInfo(
                simSlotId = slotId,
                networkType = "未知",
                isPrimary = isPrimary,
                operatorName = operatorName
            )
        }

        return CellularData(
            servingCell = servingCell,
            neighborCells = neighborCells
        )
    }

    /**
     * 获取指定 SIM 卡槽的蜂窝数据流
     * 使用 callbackFlow 监听 CellInfo 变化
     * 支持热插拔 SIM 卡的动态监听
     *
     * @param slotId SIM 卡槽 ID (0 或 1)
     * @return CellularData 流
     */
    @SuppressLint("MissingPermission")
    fun getCellularDataFlow(slotId: Int): Flow<CellularData> = callbackFlow {
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.READ_BASIC_PHONE_STATE)
            requiredPermissions.add(Manifest.permission.READ_PHONE_STATE)
        } else {
            requiredPermissions.add(Manifest.permission.READ_PHONE_STATE)
        }

        val permissionState = permissionManager.checkPermissions(requiredPermissions)

        if (!permissionState.allGranted) {
            Log.w(
                TAG,
                "权限检查失败 - SIM 卡槽: $slotId, 缺少权限: ${permissionState.missingPermissions}"
            )
            trySend(
                CellularData(
                    servingCell = CellularSignalInfo(
                        simSlotId = slotId,
                        networkType = getNetworkType(slotId),
                        isPrimary = slotId == 0
                    )
                )
            )
            awaitClose()
            return@callbackFlow
        }

        // flow 专属单线程 executor（H2 修复）：currentTm / currentCallback / lastData /
        // lteSinrFallback 的所有读写——初始注册、订阅变化处理、电话回调、awaitClose 清理——
        // 全部收敛到该线程串行执行，消除「flow 体跑在 IO 线程、系统回调跑在主线程」的竞态；
        // 副作用是把注册 / 订阅查询相关的 binder IPC 一并移出了主线程。
        val callbackExecutor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "CellularRepo-Slot$slotId").apply { isDaemon = true }
        }

        var currentTm: TelephonyManager? = null
        var currentCallback: TelephonyCallback? = null
        var lastData: CellularData? = null

        /** 最近一次「实际完成注册」的订阅 ID（M2）：只在注册成功时赋值，
         *  拔卡（无效订阅）不记录，保证重新插卡后必然重新注册。 */
        var lastRegisteredSubId = Int.MIN_VALUE

        // SINR 备用值 —— 每个 flow 各自持有。
        // 早期版本把它放在 repository 字段上，而仓库是应用级单例：双卡同时为 LTE 时，
        // 一张卡的 RSSNR 会写进共享字段被另一张卡读到（跨卡污染），且暂停采集后不清零。
        var lteSinrFallback = Int.MAX_VALUE

        // SignalStrength 备用监听器（用于 MIUI 等 CellInfo 不返回 RSSNR 的设备）。
        // PhoneStateListener / LISTEN_SIGNAL_STRENGTHS 自 API 31 起被官方弃用，指定替代品即
        // TelephonyCallback.SignalStrengthsListener，两者载荷同为 SignalStrength，
        // 因此这里与 CellInfo 走同一套 registerTelephonyCallback 注册/注销路径。
        val ssCallback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
            override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                for (css in signalStrength.cellSignalStrengths) {
                    if (css is CellSignalStrengthLte && css.rssnr != Int.MAX_VALUE) {
                        lteSinrFallback = css.rssnr
                        Log.d(TAG, "SignalStrength RSSNR 备用值更新 - SIM 卡槽: $slotId, rssnr: ${css.rssnr}")
                    }
                }
            }
        }

        /** 注销当前已注册的两个监听器。
         *  H3 修复：由 registerCallback 无条件先行调用——原先注销逻辑位于
         *  「订阅无效提前 return」之后，拔卡分支会残留旧监听器直到 flow 取消。 */
        fun unregisterCurrent() {
            currentCallback?.let {
                try {
                    currentTm?.unregisterTelephonyCallback(it)
                    Log.d(TAG, "注销旧 CellInfo 监听器 - SIM 卡槽: $slotId")
                } catch (e: Exception) {
                    Log.e(TAG, "注销 CellInfo 监听器失败 - SIM 卡槽: $slotId", e)
                }
            }
            try {
                currentTm?.unregisterTelephonyCallback(ssCallback)
            } catch (e: Exception) {
                Log.e(TAG, "注销 SignalStrength 监听器失败 - SIM 卡槽: $slotId", e)
            }
            currentCallback = null
            currentTm = null
        }

        fun registerCallback(subscriptionId: Int) {
            // M2：订阅 ID 未变化时直接跳过（订阅监听器注册后会立即回调一次，
            // 原先每次都会白跑一轮注销+重注册）
            if (subscriptionId == lastRegisteredSubId) return

            // H3 修复：注销无条件先行，任何提前 return 分支都不会残留旧监听器
            unregisterCurrent()
            // 换卡后重置 SINR 备用值，避免残留上一张卡的数据
            lteSinrFallback = Int.MAX_VALUE

            if (subscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID ||
                subscriptionId == Int.MAX_VALUE
            ) {
                Log.w(TAG, "SIM 卡未插入 - SIM 卡槽: $slotId, SubscriptionId: $subscriptionId")
                val data = CellularData(
                    servingCell = CellularSignalInfo(
                        simSlotId = slotId,
                        operatorName = "未插卡",
                        networkType = "未知",
                        isPrimary = slotId == 0
                    )
                )
                if (lastData != data) {
                    lastData = data
                    trySend(data)
                }
                return
            }

            val tm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyManager.createForSubscriptionId(subscriptionId)
            } else {
                if (slotId == 0) telephonyManager else null
            }

            if (tm == null) {
                Log.w(
                    TAG,
                    "TelephonyManager 获取失败 - SIM 卡槽: $slotId, SubscriptionId: $subscriptionId"
                )
                val data = CellularData(
                    servingCell = CellularSignalInfo(
                        simSlotId = slotId,
                        operatorName = "未插卡",
                        networkType = "未知",
                        isPrimary = slotId == 0
                    )
                )
                if (lastData != data) {
                    lastData = data
                    trySend(data)
                }
                return
            }

            val operatorName = getOperatorNameForSlot(slotId)

            val callback = object : TelephonyCallback(), TelephonyCallback.CellInfoListener {
                override fun onCellInfoChanged(cellInfoList: List<CellInfo>) {
                    Log.d(
                        TAG,
                        "CellInfo 变化回调 - SIM 卡槽: $slotId, CellInfo 数量: ${cellInfoList.size}"
                    )
                    val data = extractCellularData(cellInfoList, slotId, slotId == 0, operatorName, lteSinrFallback)
                    if (lastData != data) {
                        lastData = data
                        trySend(data)
                        Log.d(
                            TAG,
                            "数据已更新 - SIM 卡槽: $slotId, PCI: ${data.servingCell?.pci}, RSRP: ${data.servingCell?.rsrp} dBm, 运营商: ${data.servingCell?.operatorName}"
                        )
                    } else {
                        Log.d(TAG, "数据未变化 - SIM 卡槽: $slotId")
                    }
                }
            }

            try {
                tm.registerTelephonyCallback(callbackExecutor, callback)
                tm.registerTelephonyCallback(callbackExecutor, ssCallback)
                // 只有注册成功后才记录订阅 ID 与状态引用，失败路径走下方回滚
                currentTm = tm
                currentCallback = callback
                lastRegisteredSubId = subscriptionId

                val initialCellInfo = tm.allCellInfo ?: emptyList()
                Log.d(
                    TAG,
                    "初始 CellInfo - SIM 卡槽: $slotId, SubscriptionId: $subscriptionId, 数量: ${initialCellInfo.size}"
                )

                val initialData = extractCellularData(initialCellInfo, slotId, slotId == 0, operatorName, lteSinrFallback)
                if (lastData != initialData) {
                    lastData = initialData
                    trySend(initialData)
                    Log.d(
                        TAG,
                        "初始数据已发送 - SIM 卡槽: $slotId, PCI: ${initialData.servingCell?.pci}, RSRP: ${initialData.servingCell?.rsrp} dBm, 运营商: ${initialData.servingCell?.operatorName}"
                    )
                }

                Log.d(
                    TAG,
                    "注册 CellInfo 监听器成功 - SIM 卡槽: $slotId, SubscriptionId: $subscriptionId"
                )
            } catch (e: Exception) {
                Log.e(
                    TAG, "注册 CellInfo 监听器失败 - SIM 卡槽: $slotId, SubscriptionId: $subscriptionId",e
                )
                // L6：注册失败时回滚状态，避免 currentTm/currentCallback 指向未注册的回调
                unregisterCurrent()
                lastRegisteredSubId = Int.MIN_VALUE
            }
        }

        val subscriptionManager =
            context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                    as SubscriptionManager

        val subscriptionListener =
            object : SubscriptionManager.OnSubscriptionsChangedListener() {
                override fun onSubscriptionsChanged() {
                    val newSubscriptionId = getSubscriptionIdForSlot(slotId)
                    Log.d(
                        TAG,
                        "SIM 卡订阅信息变化 - SIM 卡槽: $slotId, 新 SubscriptionId: $newSubscriptionId"
                    )
                    // 本回调运行在 callbackExecutor 线程，与初始注册 / awaitClose 清理天然串行
                    registerCallback(newSubscriptionId)
                }
            }

        // minSdk 31 ≥ 30，可直接使用带 Executor 的重载，回调不再挤占主线程
        subscriptionManager.addOnSubscriptionsChangedListener(
            callbackExecutor,
            subscriptionListener
        )

        Log.d(TAG, "注册订阅监听器成功 - SIM 卡槽: $slotId")

        val initialSubscriptionId = getSubscriptionIdForSlot(slotId)
        Log.d(
            TAG,
            "初始 SubscriptionId - SIM 卡槽: $slotId, SubscriptionId: $initialSubscriptionId"
        )
        // 初始注册同样派发到 callbackExecutor（H2 修复：与回调 / 清理完全串行）
        callbackExecutor.execute { registerCallback(initialSubscriptionId) }

        awaitClose {
            // 清理派发到同一 executor 串行执行：即使注册任务仍在队列中，
            // 也必然「先注册完成、再执行清理」，不会出现注销跑在注册前面的竞态（H2 修复）；
            // 各清理项独立 try/catch，单项失败不阻断其余清理（M1 修复）。
            callbackExecutor.execute {
                unregisterCurrent()
                try {
                    subscriptionManager.removeOnSubscriptionsChangedListener(subscriptionListener)
                    Log.d(TAG, "移除订阅监听器 - SIM 卡槽: $slotId")
                } catch (e: Exception) {
                    Log.e(TAG, "移除订阅监听器失败 - SIM 卡槽: $slotId", e)
                }
                callbackExecutor.shutdown()
            }
        }
    }.distinctUntilChanged()

    /**
     * 获取双卡蜂窝数据流
     * 返回一个包含两个 SIM 卡数据的流
     *
     * @return Pair<CellularData, CellularData> 流，第一个是 SIM 1，第二个是 SIM 2
     */
    /**
     * 主动请求 Modem 刷新 CellInfo（fire-and-forget）
     *
     * 不处理返回数据——结果通过已注册的 CellInfoListener.onCellInfoChanged() 自动抵达
     * callbackFlow，最终通过 StateFlow 驱动 UI 更新。
     *
     * App 前台运行时 ViewModel 每 5s 调用一次，配合被动 Listener 实现秒级刷新。
     */
    fun requestCellInfoUpdate(slotId: Int) {
        val tm = getTelephonyManagerForSlot(slotId) ?: return
        try {
            tm.requestCellInfoUpdate(
                context.mainExecutor,
                object : TelephonyManager.CellInfoCallback() {
                    override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                        Log.d(TAG, "主动刷新成功 - SIM $slotId, cells=${cellInfo.size}")
                    }
                    override fun onError(errorCode: Int, detail: Throwable?) {
                        Log.w(TAG, "主动刷新失败 - SIM $slotId, errorCode=$errorCode")
                    }
                }
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "无权限调用 requestCellInfoUpdate - SIM $slotId", e)
        }
    }

    fun getDualSimCellularDataFlow(): Flow<Pair<CellularData, CellularData>> {
        return getCellularDataFlow(0).combine(getCellularDataFlow(1)) { sim1, sim2 ->
            sim1 to sim2
        }
    }

}