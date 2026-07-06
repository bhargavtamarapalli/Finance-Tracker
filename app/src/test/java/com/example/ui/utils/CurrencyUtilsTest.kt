package com.example.ui.utils

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CurrencyUtilsTest {

    @Before
    fun setUp() {
        // Reset to default
        CurrencyUtils.selectedCurrency = CurrencyOption.INR
    }

    @Test
    fun testFormatRupees_inr() {
        CurrencyUtils.selectedCurrency = CurrencyOption.INR
        val formatted = CurrencyUtils.formatRupees(1500.50)
        // Clean characters for assertion since currency symbols or non-breaking spaces can vary
        assertTrue(formatted.contains("1,500.50") || formatted.contains("1500.50"))
        assertTrue(formatted.contains("₹") || formatted.contains("Rs") || formatted.contains("INR"))
    }

    @Test
    fun testFormatRupees_usd() {
        CurrencyUtils.selectedCurrency = CurrencyOption.USD
        val formatted = CurrencyUtils.formatRupees(1500.50)
        assertTrue(formatted.contains("1,500.50") || formatted.contains("1500.50"))
        assertTrue(formatted.contains("$") || formatted.contains("USD"))
    }

    @Test
    fun testFormatRupees_eur() {
        CurrencyUtils.selectedCurrency = CurrencyOption.EUR
        val formatted = CurrencyUtils.formatRupees(1500.50)
        // Germany uses dot or space for thousands and comma for decimal: 1.500,50 or similar
        assertTrue(formatted.contains("1.500,50") || formatted.contains("1500,50") || formatted.contains("1,500.50") || formatted.contains("1500.50"))
        assertTrue(formatted.contains("€") || formatted.contains("EUR"))
    }

    @Test
    fun testFormatRupees_gbp() {
        CurrencyUtils.selectedCurrency = CurrencyOption.GBP
        val formatted = CurrencyUtils.formatRupees(1500.50)
        assertTrue(formatted.contains("1,500.50") || formatted.contains("1500.50"))
        assertTrue(formatted.contains("£") || formatted.contains("GBP"))
    }
}
