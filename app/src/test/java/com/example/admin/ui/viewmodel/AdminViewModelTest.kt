package com.example.admin.ui.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.admin.data.model.AdminSystemStats
import com.example.admin.data.model.AdminUserPlan
import com.example.admin.data.model.AdminUserRecord
import com.example.admin.data.model.AdminUserStatus
import com.example.data.repository.AuthRepository
import com.example.data.repository.FinanceRepository
import com.example.admin.data.repository.IAdminActionsRepository
import com.example.admin.ui.viewmodel.AdminViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AdminViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var financeRepository: FinanceRepository
    private lateinit var actionsRepository: IAdminActionsRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: AdminViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        financeRepository = mockk(relaxed = true)
        actionsRepository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)

        // Mock actions repo defaults
        coEvery { actionsRepository.isUserSuspended(any()) } returns false
        coEvery { actionsRepository.suspendUser(any(), any()) } returns Result.success(Unit)
        coEvery { actionsRepository.reactivateUser(any()) } returns Result.success(Unit)

        viewModel = AdminViewModel(financeRepository, actionsRepository, authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadStats_returnsAggregatedCounts() = runTest {
        val mockStats = AdminSystemStats(
            totalUsers = 8,
            activeUsers = 6,
            suspendedUsers = 2,
            totalTransactions = 150L,
            totalCategories = 12,
            announcementsCount = 3
        )
        coEvery { financeRepository.getSystemStats(3) } returns mockStats

        viewModel.refreshStats(3)

        val stats = viewModel.systemStats.value
        assertEquals(150L, stats?.totalTransactions)
        assertEquals(12, stats?.totalCategories)
        assertEquals(3, stats?.announcementsCount)
    }

    @Test
    fun suspendUser_togglesStatusCorrectly() = runTest {
        val testUid = "USR-10001"
        viewModel.suspendUser(testUid)

        // Check if user status updated locally in list
        val user = viewModel.allUsers.value.find { it.uid == testUid }
        assertEquals(AdminUserStatus.SUSPENDED, user?.status)
    }

    @Test
    fun reactivateUser_togglesStatusCorrectly() = runTest {
        val testUid = "USR-10002"
        viewModel.reactivateUser(testUid)

        val user = viewModel.allUsers.value.find { it.uid == testUid }
        assertEquals(AdminUserStatus.ACTIVE, user?.status)
    }

    @Test
    fun searchQuery_filtersUserList() = runTest {
        viewModel.setSearchQuery("Elena")
        val users = viewModel.filteredUsers.first()
        assertEquals(1, users.size)
        assertEquals("Elena Alistair", users.first().displayName)
    }

    @Test
    fun filterStatus_filtersPlanCorrectly() = runTest {
        viewModel.setFilterStatus(AdminUserStatus.SUSPENDED)
        val users = viewModel.filteredUsers.first()
        assertTrue(users.all { it.status == AdminUserStatus.SUSPENDED })
    }

    @Test
    fun exportReport_createsFile() = runTest {
        val testFile = File("test_report.json")
        coEvery { financeRepository.exportSystemReport(any()) } returns Result.success(testFile)

        // Set non-null stats first
        val mockStats = AdminSystemStats(8, 6, 2, 150L, 12, 3)
        coEvery { financeRepository.getSystemStats(any()) } returns mockStats
        viewModel.refreshStats(3)

        viewModel.exportReport()
        coVerify(exactly = 1) { financeRepository.exportSystemReport(any()) }
    }
}
