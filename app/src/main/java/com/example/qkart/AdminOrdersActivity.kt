package com.example.qkart

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qkart.databinding.ActivityAdminOrdersBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AdminOrdersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminOrdersBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: AdminOrdersAdapter
    private var adminShopId: String? = null
    private var adminShopName: String? = null // 🌟 Shop name ke liye naya variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Get Shop ID & Name
        val prefs = getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE)
        adminShopId = prefs.getString("SAVED_SHOP_ID", "")
        adminShopName = prefs.getString("SAVED_SHOP_NAME", "Q-Kart Shop") // 🌟 Notification ke liye name uthaya

        binding.imgOrdersBack.setOnClickListener {
            finish()
        }

        setupRecyclerView()

        if (!adminShopId.isNullOrEmpty()) {
            fetchLiveOrders()
        } else {
            Toast.makeText(this, "Shop ID not found!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = AdminOrdersAdapter { order, newStatus ->
            if (newStatus == "Set Custom Time") {
                showTimePicker(order)
            } else {
                updateStatusInFirestore(order, newStatus)
            }
        }
        binding.rvAdminOrders.layoutManager = LinearLayoutManager(this)
        binding.rvAdminOrders.adapter = adapter
    }

    // 🌟 1. NEW LOGIC: Trigger User Notification
    private fun notifyUserStatusChange(userId: String, status: String, orderId: String) {
        val userNotification = hashMapOf(
            "title" to "Order Update from $adminShopName! 🚚",
            "message" to "Your order #$orderId is now $status. Thank you for choosing us!",
            "userId" to userId,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )

        db.collection("user_notifications")
            .add(userNotification)
            .addOnSuccessListener {
                Log.d("NOTIFICATION", "User notified successfully for $status")
            }
    }

    private fun showTimePicker(order: OrderModel) {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            val amPm = if (hour < 12) "AM" else "PM"
            val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
            val selectedTime = String.format("%02d:%02d %s", displayHour, minute, amPm)

            updateStatusInFirestore(order, "Accepted", selectedTime)

        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
    }

    private fun fetchLiveOrders() {
        db.collection("orders")
            .whereEqualTo("shopId", adminShopId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("AdminOrders", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val activeOrdersList = mutableListOf<OrderModel>()
                var activeOrdersCount = 0
                var totalEarnedRevenue = 0.0
                val currentTime = System.currentTimeMillis()
                val oneHourInMillis = 3600000L

                snapshots?.forEach { doc ->
                    try {
                        val order = doc.toObject(OrderModel::class.java)
                        if (order != null) {
                            order.orderId = doc.id

                            val rawAmount = order.totalAmount?.toString() ?: "0"
                            val cleanAmount = rawAmount.replace("₹", "").replace(",", "").trim()
                            val orderPrice = cleanAmount.toDoubleOrNull() ?: 0.0

                            // Auto Cancel logic
                            if (order.status.equals("Placed", ignoreCase = true)) {
                                if ((currentTime - order.timestamp) > oneHourInMillis) {
                                    updateStatusInFirestore(order, "Rejected") // Pass order object
                                    return@forEach
                                }
                            }

                            val s = order.status.lowercase()
                            if (s != "delivered" && s != "rejected" && s != "cancelled") {
                                activeOrdersList.add(order)
                                activeOrdersCount++
                            }

                            if (s == "delivered" || s == "completed") {
                                totalEarnedRevenue += orderPrice
                            }
                        }
                    } catch (ex: Exception) {
                        Log.e("AdminOrders", "Error parsing order: ${ex.message}")
                    }
                }

                binding.tvTotalOrdersCount.text = activeOrdersCount.toString()
                binding.tvTodayRevenue.text = "₹${String.format("%.2f", totalEarnedRevenue)}"

                activeOrdersList.sortByDescending { it.timestamp }
                adapter.submitList(activeOrdersList)
            }
    }

    // 🌟 2. UPDATED: status update hote hi user notification trigger hogi
    private fun updateStatusInFirestore(order: OrderModel, newStatus: String, customTime: String = "Instant") {
        val updates = hashMapOf<String, Any>(
            "status" to newStatus,
            "deliveryTiming" to customTime
        )

        db.collection("orders").document(order.orderId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("ADMIN_UPDATE", "Order ${order.orderId} updated to $newStatus")

                // --- Notification Trigger
                if (!order.userId.isNullOrEmpty()) {
                    notifyUserStatusChange(order.userId, newStatus, order.orderId)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}