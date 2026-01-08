package com.example.qkart

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_table")
data class CartItem(
    @PrimaryKey
    var id: String = "", // 🌟 Default value di
    var name: String = "",
    var image: String = "",
    var originalPrice: Double = 0.0,
    var discountedPrice: Double = 0.0,
    var quantity: Int = 0,
    var shopId: String = ""
)