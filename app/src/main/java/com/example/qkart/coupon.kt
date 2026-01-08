
package com.example.qkart
data class Coupon(
    val id: String = "",
    val code: String = "",
    val discountPrice: Int = 0,
    val minOrderValue: Int = 0,
    val maxUsageLimit: Int = 100,
    val description: String = "",
    val shopId: String = "",
    // Kuch detail: "Flat 50 OFF"
    val timestamp: Long = System.currentTimeMillis()
)