package com.example.qkart

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

data class UserOrderModel(
    var orderId: String = "",
    var shopName: String = "",
    var items: List<CartItem> = emptyList(),
    var totalAmount: Any? = 0.0,
    var status: String = "Pending",
    var timestamp: Long = 0L,
    var userId: String = "",
    var deliveryTiming: String = "Instant"
) : Serializable {

    // --- AAPKE PURANE FUNCTIONS (NO CHANGE) ---
    fun getFormattedPrice(): String {
        return when (totalAmount) {
            is Number -> "₹${totalAmount}"
            is String -> "$totalAmount"
            else -> "₹0.0"
        }
    }

    fun getItemsAsString(): String {
        if (items.isEmpty()) return "No items"
        return items.joinToString(", ") { "${it.quantity}x ${it.name}" }
    }

    // --- NAYA LOGIC: CANCELLATION CHECK ---
    fun canCancelOrder(): Boolean {
        val s = status.lowercase()


        if (s == "delivered" || s == "cancelled" || s == "rejected" || s == "ready") {
            return false
        }


        if (deliveryTiming != "Instant" && deliveryTiming.isNotEmpty()) {
            return try {
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val deliveryTimeDate = sdf.parse(deliveryTiming)

                val now = Calendar.getInstance()
                val deliveryCal = Calendar.getInstance()
                deliveryCal.time = deliveryTimeDate!!


                deliveryCal.set(Calendar.YEAR, now.get(Calendar.YEAR))
                deliveryCal.set(Calendar.MONTH, now.get(Calendar.MONTH))
                deliveryCal.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))

                // Difference in milliseconds
                val diff = deliveryCal.timeInMillis - now.timeInMillis
                val minutesLeft = diff / (60 * 1000)


                minutesLeft > 30
            } catch (e: Exception) {

                checkBookingTimeRule()
            }
        }


        return checkBookingTimeRule()
    }

    private fun checkBookingTimeRule(): Boolean {
        val diffFromBooking = (System.currentTimeMillis() - timestamp) / (60 * 1000)
        return diffFromBooking < 10
    }
}