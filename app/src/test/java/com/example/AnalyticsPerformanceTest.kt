package com.example

import org.junit.Test
import java.util.Calendar

class AnalyticsPerformanceTest {

    @Test
    fun benchmarkCalendarInstantiation() {
        val iterations = 100000
        val dates = List(iterations) { System.currentTimeMillis() - (Math.random() * 10000000000).toLong() }

        // Baseline: instantiate Calendar inside loop
        val startTimeBaseline = System.nanoTime()
        val mapBaseline = mutableMapOf<Int, Int>()
        dates.forEach { date ->
            val cal = Calendar.getInstance().apply { timeInMillis = date }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            mapBaseline[hour] = (mapBaseline[hour] ?: 0) + 1
        }
        val durationBaselineMs = (System.nanoTime() - startTimeBaseline) / 1000000.0

        // Optimized: reuse Calendar instance
        val startTimeOptimized = System.nanoTime()
        val mapOptimized = mutableMapOf<Int, Int>()
        val reusedCal = Calendar.getInstance()
        dates.forEach { date ->
            reusedCal.timeInMillis = date
            val hour = reusedCal.get(Calendar.HOUR_OF_DAY)
            mapOptimized[hour] = (mapOptimized[hour] ?: 0) + 1
        }
        val durationOptimizedMs = (System.nanoTime() - startTimeOptimized) / 1000000.0

        println("Baseline (ms): $durationBaselineMs")
        println("Optimized (ms): $durationOptimizedMs")
        println("Improvement: ${"%.2f".format((durationBaselineMs - durationOptimizedMs) / durationBaselineMs * 100)}%")
    }
}
