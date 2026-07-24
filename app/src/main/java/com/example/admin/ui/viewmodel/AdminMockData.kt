package com.example.admin.ui.viewmodel

import com.example.admin.data.model.AdminUserPlan
import com.example.admin.data.model.AdminUserRecord
import com.example.admin.data.model.AdminUserStatus

/**
 * Seed data for development and testing environments.
 * Matches the layout and details of the Obsidian Web Admin Panel mockup.
 */
object AdminMockData {
    private val now = System.currentTimeMillis()
    private val hourMs = 3600000L
    private val dayMs = 86400000L

    val users: List<AdminUserRecord> = listOf(
        AdminUserRecord(
            uid = "USR-10001",
            displayName = "Elena Alistair",
            email = "elena@obsidian.io",
            status = AdminUserStatus.ACTIVE,
            plan = AdminUserPlan.BUSINESS,
            joinedAt = now - 280 * dayMs,
            lastActiveAt = now - 2 * 60000L,
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
            joinedAt = now - 120 * dayMs,
            lastActiveAt = now - 15 * dayMs,
            sessionCount = 12,
            region = "US — New York",
            deviceInfo = "Windows / Edge 119"
        ),
        AdminUserRecord(
            uid = "USR-10003",
            displayName = "Sarah Jenkins",
            email = "sarah.j@global.net",
            status = AdminUserStatus.ACTIVE,
            plan = AdminUserPlan.BUSINESS,
            joinedAt = now - 350 * dayMs,
            lastActiveAt = now - 1 * hourMs,
            sessionCount = 104,
            region = "UK — London",
            deviceInfo = "macOS / Safari 17"
        ),
        AdminUserRecord(
            uid = "USR-10004",
            displayName = "Julianne West",
            email = "j.west@techflow.io",
            status = AdminUserStatus.ACTIVE,
            plan = AdminUserPlan.BUSINESS,
            joinedAt = now - 80 * dayMs,
            lastActiveAt = now - 1 * dayMs,
            sessionCount = 29,
            region = "SG — Singapore",
            deviceInfo = "iOS / Safari 17"
        ),
        AdminUserRecord(
            uid = "USR-10005",
            displayName = "Ravi Patel",
            email = "ravi.p@startup.io",
            status = AdminUserStatus.ACTIVE,
            plan = AdminUserPlan.BASIC,
            joinedAt = now - 45 * dayMs,
            lastActiveAt = now - 3 * hourMs,
            sessionCount = 63,
            region = "IN — Bengaluru",
            deviceInfo = "Android / Chrome 118"
        ),
        AdminUserRecord(
            uid = "USR-10006",
            displayName = "Lyra Steinfeld",
            email = "lyra@nordic.eu",
            status = AdminUserStatus.ACTIVE,
            plan = AdminUserPlan.BUSINESS,
            joinedAt = now - 60 * dayMs,
            lastActiveAt = now - 2 * dayMs,
            sessionCount = 87,
            region = "SE — Stockholm",
            deviceInfo = "macOS / Firefox 120"
        ),
        AdminUserRecord(
            uid = "USR-10007",
            displayName = "Omar Kassim",
            email = "o.kassim@fintech.ae",
            status = AdminUserStatus.SUSPENDED,
            plan = AdminUserPlan.BUSINESS,
            joinedAt = now - 90 * dayMs,
            lastActiveAt = now - 5 * dayMs,
            sessionCount = 8,
            region = "AE — Dubai",
            deviceInfo = "Windows / Chrome 118"
        ),
        AdminUserRecord(
            uid = "USR-10008",
            displayName = "Priya Sharma",
            email = "priya.s@enterprise.com",
            status = AdminUserStatus.ACTIVE,
            plan = AdminUserPlan.BUSINESS,
            joinedAt = now - 180 * dayMs,
            lastActiveAt = now - 30 * 60000L,
            sessionCount = 211,
            region = "IN — Delhi",
            deviceInfo = "macOS / Chrome 119"
        )
    )
}
