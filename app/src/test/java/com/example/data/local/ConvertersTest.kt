package com.example.data.local

import com.example.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun testFromTransactionType() {
        assertEquals("INCOME", converters.fromTransactionType(TransactionType.INCOME))
        assertEquals("EXPENSE", converters.fromTransactionType(TransactionType.EXPENSE))
    }

    @Test
    fun testToTransactionType() {
        assertEquals(TransactionType.INCOME, converters.toTransactionType("INCOME"))
        assertEquals(TransactionType.EXPENSE, converters.toTransactionType("EXPENSE"))
    }
}
