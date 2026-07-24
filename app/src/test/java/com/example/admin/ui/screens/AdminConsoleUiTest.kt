package com.example.admin.ui.screens

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.admin.data.model.AdminSystemStats
import com.example.admin.data.model.AdminUserPlan
import com.example.admin.data.model.AdminUserRecord
import com.example.admin.data.model.AdminUserStatus
import com.example.admin.ui.screens.AdminDashboardTab
import com.example.admin.ui.screens.AdminUserCard
import com.example.admin.ui.screens.AdminUserProfileSheet
import com.example.admin.ui.screens.AdminUsersTab
import com.example.ui.theme.FinanceTrackerTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [33])
class AdminConsoleUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testStats = AdminSystemStats(
        totalUsers = 8,
        activeUsers = 6,
        suspendedUsers = 2,
        totalTransactions = 150L,
        totalCategories = 12,
        announcementsCount = 3
    )

    private val testUsers = listOf(
        AdminUserRecord(
            uid = "USR-10001",
            displayName = "Elena Alistair",
            email = "elena@obsidian.io",
            status = AdminUserStatus.ACTIVE,
            plan = AdminUserPlan.BUSINESS,
            joinedAt = System.currentTimeMillis() - 10000,
            lastActiveAt = System.currentTimeMillis() - 1000,
            sessionCount = 47,
            region = "IN — Mumbai",
            deviceInfo = "macOS / Chrome 118"
        ),
        AdminUserRecord(
            uid = "USR-10002",
            displayName = "Marcus Thorne",
            email = "m.thorne@vortex.com",
            status = AdminUserStatus.SUSPENDED,
            plan = AdminUserPlan.BASIC,
            joinedAt = System.currentTimeMillis() - 20000,
            lastActiveAt = System.currentTimeMillis() - 2000,
            sessionCount = 12,
            region = "US — New York",
            deviceInfo = "Windows / Edge 119"
        )
    )

    @Test
    fun dashboardTab_rendersAllDiagnosticsAndStats() {
        var exportClicked = false
        var purgeClicked = false

        composeTestRule.setContent {
            FinanceTrackerTheme {
                AdminDashboardTab(
                    stats = testStats,
                    announcements = emptyList(),
                    onExportReport = { exportClicked = true },
                    onClearAllData = { purgeClicked = true }
                )
            }
        }

        // Verify stat cards display values
        composeTestRule.onNodeWithTag("stat_total_users").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stat_active_users").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stat_total_transactions").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stat_total_announcements").assertIsDisplayed()

        // Click export report
        composeTestRule.onNodeWithTag("btn_export_report").performClick()
        assertTrue(exportClicked)
    }

    @Test
    fun usersTab_filtersByChipSelections() {
        var searchChange = ""
        var statusFilter: AdminUserStatus? = null

        composeTestRule.setContent {
            FinanceTrackerTheme {
                AdminUsersTab(
                    users = testUsers,
                    searchQuery = "",
                    filterStatus = null,
                    filterPlan = null,
                    onSearchChange = { searchChange = it },
                    onStatusFilterChange = { statusFilter = it },
                    onPlanFilterChange = {},
                    onSuspendToggle = {},
                    onResetPassword = {},
                    onViewProfile = {},
                    onDeleteUser = {}
                )
            }
        }

        // Verify search field exists
        composeTestRule.onNodeWithTag("user_search_field").performTextInput("Elena")
        assertEquals("Elena", searchChange)

        // Click active chip filter
        composeTestRule.onNodeWithTag("chip_status_active").performClick()
        assertEquals(AdminUserStatus.ACTIVE, statusFilter)
    }

    @Test
    fun profileSheet_displaysUserInfoAndActions() {
        var suspendClicked = false
        var dismissClicked = false

        composeTestRule.setContent {
            FinanceTrackerTheme {
                AdminUserProfileSheet(
                    user = testUsers.first(),
                    onSuspendToggle = { suspendClicked = true },
                    onResetPassword = {},
                    onDelete = {},
                    onDismiss = { dismissClicked = true }
                )
            }
        }

        // Verify profile sheet contents
        composeTestRule.onNodeWithTag("admin_user_profile_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Elena Alistair").assertIsDisplayed()
        composeTestRule.onNodeWithText("elena@obsidian.io").assertIsDisplayed()

        // Trigger suspend action from bottom sheet
        composeTestRule.onNodeWithTag("btn_sheet_suspend").performClick()
        
        // Assert confirmation dialog appears
        composeTestRule.onNodeWithText("Confirm Suspend").performClick()
        assertTrue(suspendClicked)
    }
}
