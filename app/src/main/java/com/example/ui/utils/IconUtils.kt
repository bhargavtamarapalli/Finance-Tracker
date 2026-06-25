package com.example.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun getIconByName(name: String): ImageVector {
    return when (name) {
        "restaurant" -> Icons.Default.Restaurant
        "shopping_cart" -> Icons.Default.ShoppingCart
        "directions_bus" -> Icons.Default.DirectionsBus
        "local_gas_station" -> Icons.Default.LocalGasStation
        "home" -> Icons.Default.Home
        "bolt" -> Icons.Default.Bolt
        "attach_money" -> Icons.Default.AttachMoney
        "work" -> Icons.Default.Work
        "store" -> Icons.Default.Store
        "more_horiz" -> Icons.Default.MoreHoriz
        "percent" -> Icons.Default.Percent
        "trending_up" -> Icons.Default.TrendingUp
        "keyboard_return" -> Icons.Default.KeyboardReturn
        "card_giftcard" -> Icons.Default.CardGiftcard
        "shopping_bag" -> Icons.Default.ShoppingBag
        "movie" -> Icons.Default.Movie
        "flight" -> Icons.Default.Flight
        "medical_services" -> Icons.Default.MedicalServices
        "shield" -> Icons.Default.Shield
        "school" -> Icons.Default.School
        "credit_card" -> Icons.Default.CreditCard
        "show_chart" -> Icons.Default.ShowChart
        else -> Icons.Default.Category
    }
}
