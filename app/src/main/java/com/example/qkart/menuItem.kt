package com.example.qkart

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
@Entity(tableName = "menu_table") // Room Table Name
data class MenuItem(
    @PrimaryKey
    @DocumentId
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var image: String = "",
    var price: Double = 0.0,

    @field:JvmField
    var isAvailable: Boolean = true,

    var rating: Double = 0.0,
    var offer: String = "",
    var shopId: String = "",
    var discountedPrice: Double = 0.0,


    // 🌟 Room logic ke liye: Track karega ki item Normal hai ya Special
    var isSpecialItem: Boolean = false,

    @Ignore
    var localQuantity: Int = 0
) {
    // Room aur Firebase dono ke liye Empty Constructor
    constructor() : this("", "", "", "", 0.0, true, 0.0, "", "", 0.0, false, localQuantity = 0)

fun calculateDiscountedPriceFromOffer(): Double {
    // Agar offer khali hai ya usme % nahi hai toh discountedPrice hi use karein ya base price
    if (offer.isNullOrEmpty() || !offer.contains("%")) {
        return if (discountedPrice > 0) discountedPrice else price
    }

    return try {
        // Regex jo sirf numbers nikalega (e.g., "20% OFF" -> "20")
        val digitRegex = Regex("(\\d+)")
        val match = digitRegex.find(offer)
        val percentage = match?.value?.toDouble() ?: 0.0

        if (percentage > 0) {
            val discountAmount = (price * percentage) / 100
            price - discountAmount
        } else {
            price
        }
    } catch (e: Exception) {
        price
    }
}
}