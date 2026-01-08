package com.example.qkart
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "shop_table")
data class Shop(
    @PrimaryKey var id: String = "",
    var name: String = "",
    var description: String = "",
    var image: String = "",
    var rating: Double = 0.0,
    @get:JvmName("getIsAvailable") var isAvailable: Boolean = true, // 🌟 Is line ko aise hi likho
    var prepTime: String = "20 mins",
    var shopTime: String = "9 AM - 10 PM",
    val adminKey: String = ""
) {
    constructor() : this("", "", "", "", 0.0, true, "", "", adminKey = "")
}