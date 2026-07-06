package com.example.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IconUtilsTest {

    @Test
    fun testGetIconByName_matchingIcons() {
        assertEquals(Icons.Default.Restaurant, getIconByName("restaurant"))
        assertEquals(Icons.Default.ShoppingCart, getIconByName("shopping_cart"))
    }

    @Test
    fun testGetIconByName_fallback() {
        // Unknown icon name should fallback to default Category icon
        assertEquals(Icons.Default.Category, getIconByName("non_existent_icon_name_123"))
    }
}
