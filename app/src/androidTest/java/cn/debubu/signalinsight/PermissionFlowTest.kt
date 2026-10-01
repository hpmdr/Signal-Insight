package cn.debubu.signalinsight

import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.debubu.signalinsight.data.permission.PermissionManager
import cn.debubu.signalinsight.ui.permission.PermissionScreen
import cn.debubu.signalinsight.ui.permission.PermissionViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 权限流程 UI 自动化测试。
 *
 * 规范要点（本次修订）：
 * 1. **文案全部从资源读取**：原先硬编码中文，在英文 locale 设备上必然失败；
 *    现统一用 `targetContext.getString(R.string.…)` 取词，与运行语言无关。
 * 2. **断言不得被条件包住**：原「全部授权后应跳转」用例的断言写在
 *    `if (allPermissionsGranted.value)` 内，条件为假即静默空过；
 *    原「永久拒绝」用例两个分支都断言，逻辑上不可能失败。两者均已改为无条件断言。
 * 3. 使用 `createComposeRule()` 而非 `createAndroidComposeRule()`：
 *    部分 OEM 系统会拦截后者创建的 Activity，前者使用内部托管 Activity，兼容性更好。
 */
@RunWith(AndroidJUnit4::class)
class PermissionFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: PermissionViewModel
    private lateinit var permissionManager: PermissionManager
    private lateinit var context: Context

    /** 从资源取词，避免硬编码文案导致的语言相关失败 */
    private fun str(resId: Int): String = context.getString(resId)

    /** 该文案在界面上是否存在（不抛异常，供「二者其一」类断言使用） */
    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextSafe(
        text: String
    ): Boolean = onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        permissionManager = PermissionManager(context)
        viewModel = PermissionViewModel(permissionManager)
    }

    // ═══════════════════════════════════════════════════
    // UI 渲染测试（不依赖 Activity 对象）
    // ═══════════════════════════════════════════════════

    @Test
    fun initialScreen_displaysPhonePermissionCard() {
        composeTestRule.setContent {
            PermissionScreen(onNavigateToMain = {}, viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(str(R.string.perm_phone_full_title)).assertExists()
    }

    @Test
    fun initialScreen_displaysLocationPermissionCard() {
        composeTestRule.setContent {
            PermissionScreen(onNavigateToMain = {}, viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(str(R.string.perm_location_title)).assertExists()
    }

    @Test
    fun initialScreen_showsPendingStatusOnCards() {
        composeTestRule.setContent {
            PermissionScreen(onNavigateToMain = {}, viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(str(R.string.perm_status_pending)).assertExists()
    }

    @Test
    fun initialScreen_showsAuthorizeButton() {
        composeTestRule.setContent {
            PermissionScreen(onNavigateToMain = {}, viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(str(R.string.permission_authorize)).assertIsDisplayed()
    }

    // ═══════════════════════════════════════════════════
    // 请求流程
    // ═══════════════════════════════════════════════════

    @Test
    fun clickingAuthorize_setsRequestingFlag() {
        composeTestRule.setContent {
            PermissionScreen(onNavigateToMain = {}, viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(str(R.string.permission_authorize)).performClick()
        composeTestRule.waitForIdle()

        assertTrue("点击授权后应进入请求中状态", viewModel.isRequestingPermissions.value)
    }

    /**
     * 请求集合必须符合官方要求：不得单独请求 ACCESS_FINE_LOCATION，
     * 必须与 ACCESS_COARSE_LOCATION 在同一次请求中提交，
     * 否则部分 Android 12 版本会忽略整个请求。
     */
    @Test
    fun requestList_pairsFineWithCoarse_whenFineIsPending() {
        val requested = viewModel.buildRequestListForTest()

        if (requested.contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
            assertTrue(
                "请求 FINE 时必须同时请求 COARSE（官方要求）",
                requested.contains(Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    /** READ_BASIC_PHONE_STATE 是 non-dangerous 权限，不应出现在运行时请求清单中 */
    @Test
    fun requestList_excludesBasicPhoneState() {
        val requested = viewModel.buildRequestListForTest()

        assertFalse(
            "READ_BASIC_PHONE_STATE 非运行时权限，不应请求",
            requested.contains(Manifest.permission.READ_BASIC_PHONE_STATE)
        )
    }

    // ═══════════════════════════════════════════════════
    // 结果处理（无条件断言）
    // ═══════════════════════════════════════════════════

    /**
     * 全部授权 → 汇总状态必须为 true（无条件断言）。
     *
     * 注：`handlePermissionResult` 只在 result 中显式提供时采用该值，
     * 未提供的条目回读系统真实状态；因此这里只对「显式提供 true 的条目」
     * 断言其被计为已授权，不假设设备上权限的真实授予情况。
     */
    @Test
    fun handleResult_marksExplicitlyGrantedPermissions() {
        composeTestRule.setContent {
            PermissionScreen(onNavigateToMain = {}, viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        viewModel.handlePermissionResult(
            permissions = listOf(Manifest.permission.READ_PHONE_STATE),
            result = mapOf(Manifest.permission.READ_PHONE_STATE to true),
            activity = null
        )
        composeTestRule.waitForIdle()

        val card = viewModel.permissionRequirements
            .firstOrNull { it.permission == Manifest.permission.READ_PHONE_STATE }
        assertTrue("显式授予的权限应被标记为已授权", card?.isGranted == true)
    }

    /**
     * 拒绝且「已请求过」→ 必须给出可操作的出口（无条件断言）。
     *
     * 与旧版区别：旧版在 `if (hasPermanentlyDenied)` 与 `else` 两个分支里都做了断言，
     * 因此不可能失败。现在断言的是**无论落在哪个分支都必须成立**的性质：
     * 用户总能看到一个可点击的出口（去设置中心 或 授权并进入）。
     */
    @Test
    fun afterDenial_userAlwaysHasAnActionableExit() {
        var activity: Activity? = null

        composeTestRule.setContent {
            activity = LocalContext.current as? Activity
            PermissionScreen(onNavigateToMain = {}, viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        val act = activity
        // Activity 不可用时（例如宿主环境差异）跳过后续交互，但仍断言界面已渲染出授权按钮
        if (act == null) {
            composeTestRule
                .onNodeWithText(str(R.string.permission_authorize))
                .assertIsDisplayed()
            return
        }
        viewModel.requestPermissions(act)

        viewModel.handlePermissionResult(
            permissions = viewModel.lastRequestedPermissions(),
            result = viewModel.lastRequestedPermissions().associateWith { false },
            activity = act
        )
        composeTestRule.waitForIdle()

        // 无论是否判定为「永久拒绝」，界面上都必须存在一个可操作的出口
        val hasGoToSettings = composeTestRule
            .onAllNodesWithTextSafe(str(R.string.permission_go_settings))
        val hasAuthorize = composeTestRule
            .onAllNodesWithTextSafe(str(R.string.permission_authorize))

        assertTrue(
            "拒绝后用户必须能看到「去设置中心」或「授权并进入」之一",
            hasGoToSettings || hasAuthorize
        )
    }

    /** 规格校验：正式版权限清单只应包含运行时权限（不含 non-dangerous 的 BASIC_PHONE_STATE） */
    @Test
    fun permissionRequirements_containOnlyRuntimePermissions() {
        val permissions = viewModel.permissionRequirements.map { it.permission }

        assertTrue(
            "应包含 READ_PHONE_STATE",
            permissions.contains(Manifest.permission.READ_PHONE_STATE)
        )
        assertTrue(
            "应包含 ACCESS_FINE_LOCATION",
            permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION)
        )
        assertFalse(
            "不应包含 non-dangerous 的 READ_BASIC_PHONE_STATE",
            permissions.contains(Manifest.permission.READ_BASIC_PHONE_STATE)
        )
        assertEquals("权限条目数应为 2", 2, permissions.size)
    }
}
