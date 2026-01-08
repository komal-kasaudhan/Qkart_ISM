package com.example.qkart

data class OrderModel(
    var orderId: String = "",
    val userId: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val userAltPhone: String = "",
    val userHostel: String = "",
    val userRoom: String = "",
    val items: List<CartItem> = emptyList(),

    // 🌟 CHANGE: Ise Any? rahein taaki String aur Double dono handle ho sakein
    val totalAmount: Any? = 0.0,

    val deliveryTiming: String = "Instant",
    val paymentMode: String = "COD",
    var status: String = "Placed",
    val timestamp: Long = 0L
) {
    // 🌟 EXTRA SAFE LOGIC:
    // Is function ko use karein jahan bhi Price dikhani ho
    fun getTotalAmountValue(): Double {
        return when (totalAmount) {
            is Number -> totalAmount.toDouble()
            is String -> totalAmount.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }
}