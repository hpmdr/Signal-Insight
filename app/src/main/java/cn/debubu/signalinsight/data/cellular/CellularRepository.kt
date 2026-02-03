package cn.debubu.signalinsight.data.cellular

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.CellInfo
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

        val activeSubscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
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

        val activeSubscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
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

    /**
     * 获取网络类型
     *
     * @param slotId SIM 卡槽 ID
     * @return 网络类型字符串
     */
    private fun getNetworkType(slotId: Int): String {
        val tm = getTelephonyManagerForSlot(slotId) ?: return "未知"
        return CellularSignalInfo.getNetworkTypeName(tm.networkType)
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
        operatorName: String = "未知"
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
                servingCell = CellularSignalInfo.fromCellInfo(cellInfo, slotId, isPrimary, operatorName)
                Log.d(
                    TAG,
                    "服务小区信息 - SIM 卡槽: $slotId, PCI: ${servingCell.pci}, RSRP: ${servingCell.rsrp} dBm, 运营商: ${servingCell.operatorName}, 频段: ${servingCell.band}, 网络类型: ${servingCell.networkType}"
                )
            } else {
                val neighborCell = NeighborCellInfo.fromCellInfo(cellInfo, isServing = false)
                neighborCells.add(neighborCell)
                Log.d(
                    TAG,
                    "邻小区信息 - SIM 卡槽: $slotId, PCI: ${neighborCell.pci}, RSRP: ${neighborCell.rsrp} dBm, 频段: ${neighborCell.band}"
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
     * 获取指定 SIM 卡槽的蜂窝数据
     *
     * @param slotId SIM 卡槽 ID (0 或 1)
     * @return CellularData 对象
     */
    @SuppressLint("MissingPermission")
    fun getCellularData(slotId: Int): CellularData {
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
            return CellularData(
                servingCell = CellularSignalInfo(
                    simSlotId = slotId,
                    networkType = getNetworkType(slotId),
                    isPrimary = slotId == 0,
                    operatorName = getOperatorNameForSlot(slotId)
                )
            )
        }

        val tm = getTelephonyManagerForSlot(slotId)
        val cellInfoList = tm?.allCellInfo ?: emptyList()

        Log.d(TAG, "获取蜂窝数据 - SIM 卡槽: $slotId, CellInfo 数量: ${cellInfoList.size}")

        return extractCellularData(cellInfoList, slotId, slotId == 0, getOperatorNameForSlot(slotId))
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

        var currentTm: TelephonyManager? = null
        var currentCallback: TelephonyCallback? = null
        var lastData: CellularData? = null

        fun registerCallback(subscriptionId: Int) {
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

            currentCallback?.let {
                try {
                    currentTm?.unregisterTelephonyCallback(it)
                    Log.d(TAG, "注销旧监听器 - SIM 卡槽: $slotId")
                } catch (e: Exception) {
                    Log.e(TAG, "注销 CellInfo 监听器失败 - SIM 卡槽: $slotId",e)
                }
            }

            currentTm = tm

            val operatorName = getOperatorNameForSlot(slotId)

            val callback = object : TelephonyCallback(), TelephonyCallback.CellInfoListener {
                override fun onCellInfoChanged(cellInfoList: List<CellInfo>) {
                    Log.d(
                        TAG,
                        "CellInfo 变化回调 - SIM 卡槽: $slotId, CellInfo 数量: ${cellInfoList.size}"
                    )
                    val data = extractCellularData(cellInfoList, slotId, slotId == 0, operatorName)
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

            currentCallback = callback

            try {
                tm.registerTelephonyCallback(context.mainExecutor, callback)

                val initialCellInfo = tm.allCellInfo ?: emptyList()
                Log.d(
                    TAG,
                    "初始 CellInfo - SIM 卡槽: $slotId, SubscriptionId: $subscriptionId, 数量: ${initialCellInfo.size}"
                )

                val initialData = extractCellularData(initialCellInfo, slotId, slotId == 0, operatorName)
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
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
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
                        registerCallback(newSubscriptionId)
                    }
                }

            subscriptionManager.addOnSubscriptionsChangedListener(
                context.mainExecutor,
                subscriptionListener
            )

            Log.d(TAG, "注册订阅监听器成功 - SIM 卡槽: $slotId")

            val initialSubscriptionId = getSubscriptionIdForSlot(slotId)
            Log.d(
                TAG,
                "初始 SubscriptionId - SIM 卡槽: $slotId, SubscriptionId: $initialSubscriptionId"
            )
            registerCallback(initialSubscriptionId)

            awaitClose {
                try {
                    currentCallback?.let {
                        currentTm?.unregisterTelephonyCallback(it)
                        Log.d(TAG, "注销 CellInfo 监听器 - SIM 卡槽: $slotId")
                    }
                    subscriptionManager.removeOnSubscriptionsChangedListener(subscriptionListener)
                    Log.d(TAG, "移除订阅监听器 - SIM 卡槽: $slotId")
                } catch (e: Exception) {
                    Log.e(TAG,  "注销监听器失败 - SIM 卡槽: $slotId",e)
                }
            }
        } else {
            val initialSubscriptionId = getSubscriptionIdForSlot(slotId)
            Log.d(
                TAG,
                "初始 SubscriptionId (Android N 以下) - SIM 卡槽: $slotId, SubscriptionId: $initialSubscriptionId"
            )
            registerCallback(initialSubscriptionId)

            awaitClose {
                try {
                    currentCallback?.let {
                        currentTm?.unregisterTelephonyCallback(it)
                        Log.d(TAG, "注销 CellInfo 监听器 - SIM 卡槽: $slotId")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "注销监听器失败 - SIM 卡槽: $slotId", e)
                }
            }
        }
    }.distinctUntilChanged()

    /**
     * 获取双卡蜂窝数据流
     * 返回一个包含两个 SIM 卡数据的流
     *
     * @return Pair<CellularData, CellularData> 流，第一个是 SIM 1，第二个是 SIM 2
     */
    fun getDualSimCellularDataFlow(): Flow<Pair<CellularData, CellularData>> {
        return getCellularDataFlow(0).combine(getCellularDataFlow(1)) { sim1, sim2 ->
            sim1 to sim2
        }
    }

}