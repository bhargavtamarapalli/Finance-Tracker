package com.example.ui.utils

import java.text.NumberFormat
import java.util.Locale

enum class CurrencyOption(val code: String, val symbol: String, val label: String, val locale: Locale, val isFuture: Boolean) {
    INR("INR", "₹", "INR (₹)", Locale("en", "IN"), false),
    USD("USD", "$", "USD ($)", Locale.US, true),
    EUR("EUR", "€", "EUR (€)", Locale.GERMANY, true),
    GBP("GBP", "£", "GBP (£)", Locale.UK, true)
}

object CurrencyUtils {
    var selectedCurrency: CurrencyOption = CurrencyOption.INR

    fun formatRupees(amount: Double): String {
        return try {
            val formatter = NumberFormat.getCurrencyInstance(selectedCurrency.locale)
            formatter.format(amount)
        } catch (e: Exception) {
            // Fallback to INR in case of locale rendering issues
            val fallbackFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            fallbackFormatter.format(amount)
        }
    }
}
