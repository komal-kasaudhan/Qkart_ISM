package com.example.qkart

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "pickup_profiles")
data class PickupProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val altPhone: String,
    val hostel: String,
    val roomNo: String,
    var isSelected: Boolean = false // Taaki pata chale kaunsa select hua hai
)



