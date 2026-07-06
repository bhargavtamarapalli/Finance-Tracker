package com.example.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import org.junit.Assert.assertEquals
import org.junit.Test

class IconUtilsTest {

    @Test
    fun testGetIconByName_matchingIcons() {
        assertEquals(Icons.Default.Restaurant, getIconByName("restaurant"))
        assertEquals(Icons.Default.ShoppingCart, getIconByName("shopping_cart"))
        assertEquals(Icons.Default.DirectionsBus, getIconByName("directions_bus"))
        assertEquals(Icons.Default.LocalGasStation, getIconByName("local_gas_station"))
        assertEquals(Icons.Default.Home, getIconByName("home"))
        assertEquals(Icons.Default.Bolt, getIconByName("bolt"))
        assertEquals(Icons.Default.AttachMoney, getIconByName("attach_money"))
        assertEquals(Icons.Default.Work, getIconByName("work"))
        assertEquals(Icons.Default.Store, getIconByName("store"))
        assertEquals(Icons.Default.MoreHoriz, getIconByName("more_horiz"))
        assertEquals(Icons.Default.Percent, getIconByName("percent"))
        assertEquals(Icons.Default.TrendingUp, getIconByName("trending_up"))
        assertEquals(Icons.Default.KeyboardReturn, getIconByName("keyboard_return"))
        assertEquals(Icons.Default.CardGiftcard, getIconByName("card_giftcard"))
        assertEquals(Icons.Default.ShoppingBag, getIconByName("shopping_bag"))
        assertEquals(Icons.Default.Movie, getIconByName("movie"))
        assertEquals(Icons.Default.Flight, getIconByName("flight"))
        assertEquals(Icons.Default.MedicalServices, getIconByName("medical_services"))
        assertEquals(Icons.Default.Shield, getIconByName("shield"))
        assertEquals(Icons.Default.School, getIconByName("school"))
        assertEquals(Icons.Default.CreditCard, getIconByName("credit_card"))
        assertEquals(Icons.Default.ShowChart, getIconByName("show_chart"))
    }

    @Test
    fun testGetIconByName_fallback() {
        // Unknown icon name should fallback to default Category icon
        assertEquals(Icons.Default.Category, getIconByName("non_existent_icon_name_123"))
        assertEquals(Icons.Default.Category, getIconByName(""))
    }
}
