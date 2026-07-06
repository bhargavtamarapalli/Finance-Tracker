package com.example.ui.utils

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun testFormatRupees_negativeValue() {
        CurrencyUtils.selectedCurrency = CurrencyOption.USD
        val formatted = CurrencyUtils.formatRupees(-25.75)
        assertTrue(formatted.contains("25.75"))
        assertTrue(formatted.contains("-") || formatted.contains("(")) // Accounting for accounting representation
    }

    @Test
    fun testFormatRupees_largeValue() {
        CurrencyUtils.selectedCurrency = CurrencyOption.INR
        val formatted = CurrencyUtils.formatRupees(10000000.0)
        assertTrue(formatted.contains("10,000,000") || formatted.contains("1,00,00,000") || formatted.contains("10000000"))
    }

    @Test
    fun testCurrencyOption_properties() {
        val option = CurrencyOption.INR
        assertEquals("INR", option.code)
        assertEquals("₹", option.symbol)
        assertEquals("INR (₹)", option.label)
        assertFalse(option.isFuture)
    }
}
