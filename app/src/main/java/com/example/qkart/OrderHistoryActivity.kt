package com.example.qkart

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrderHistoryActivity : AppCompatActivity() {

    private lateinit var rvLive: RecyclerView
    private lateinit var rvPast: RecyclerView

    private var liveAdapter: OrderAdapter? = null
    private var pastAdapter: OrderAdapter? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_history)

        try {
            rvLive = findViewById(R.id.rv_live_orders)
            rvPast = findViewById(R.id.rv_past_orders)

            rvLive.layoutManager = LinearLayoutManager(this)
            rvPast.layoutManager = LinearLayoutManager(this)

            setupInitialAdapters()
            fetchOrders()

        } catch (e: Exception) {
            Log.e("CRASH_ERROR", "Initialization Error: ${e.message}")
            Toast.makeText(this, "UI Error: Check Layout IDs", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupInitialAdapters() {
        // Live Orders Adapter - Reorder click pe kuch nahi karega
        liveAdapter = OrderAdapter(this, ArrayList(),
            { order -> showCancelConfirmation(order) },
            { order -> showDeleteConfirmation(order) },
            { /* Live order reorder nahi hoga */ })
        rvLive.adapter = liveAdapter

        // Past Orders Adapter - Yahan Reorder logic pass kiya
        pastAdapter = OrderAdapter(this, ArrayList(),
            { /* No cancel */ },
            { order -> showDeleteConfirmation(order) },
            { order -> showReorderConfirmation(order) }) // 🌟 Reorder trigger
        rvPast.adapter = pastAdapter
    }

    private fun fetchOrders() {
        val currentUser = auth.currentUser?.uid ?: return

        db.collection("orders")
            .whereEqualTo("userId", currentUser)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIRESTORE_ERROR", "Error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val myOrders = mutableListOf<UserOrderModel>()

                    for (doc in snapshot.documents) {
                        try {
                            val order = doc.toObject(UserOrderModel::class.java)
                            if (order != null) {
                                order.orderId = doc.id
                                myOrders.add(order)
                            }
                        } catch (e: Exception) {
                            Log.e("MAPPING_ERROR", "Model mapping failed: ${e.message}")
                        }
                    }

                    val liveList = myOrders.filter {
                        val s = it.status.lowercase().trim()
                        s == "pending" || s == "placed" || s == "accepted" || s == "preparing" || s == "ready" || s == "out for delivery"
                    }.sortedByDescending { it.timestamp }

                    val pastList = myOrders.filter {
                        val s = it.status.lowercase().trim()
                        s == "delivered" || s == "rejected" || s == "cancelled"
                    }.sortedByDescending { it.timestamp }

                    liveAdapter?.updateData(liveList)
                    pastAdapter?.updateData(pastList)
                }
            }
    }

    // 🌟 REORDER LOGIC FUNCTIONS
    private fun showReorderConfirmation(order: UserOrderModel) {
        AlertDialog.Builder(this)
            .setTitle("Reorder")
            .setMessage("Do you want to add all items in the cart?")
            .setPositiveButton("Yes") { _, _ ->
                reorderItemsToCart(order)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun reorderItemsToCart(order: UserOrderModel) {
        // 🌟 Error yahan ho sakti hai agar lifecycleScope use nahi kiya
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@OrderHistoryActivity)
                val cartDao = db.cartDao()

                // Purane items ko Cart mein insert karna
                order.items.forEach { item ->
                    cartDao.insert(item) // 🌟 Room ka insert function yahan call hoga
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OrderHistoryActivity, "Items added to cart!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@OrderHistoryActivity, CartActivity::class.java)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("REORDER_ERROR", e.message.toString())
                    Toast.makeText(this@OrderHistoryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- BAKI PURANE FUNCTIONS (NO CHANGE) ---
    private fun showCancelConfirmation(order: UserOrderModel) {
        AlertDialog.Builder(this)
            .setTitle("Cancel Order")
            .setMessage("Do you want to cancel this order?")
            .setPositiveButton("Yes") { _, _ ->
                db.collection("orders").document(order.orderId)
                    .update("status", "Cancelled")
                    .addOnSuccessListener {
                        Toast.makeText(this, "Order Cancelled", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showDeleteConfirmation(order: UserOrderModel) {
        AlertDialog.Builder(this)
            .setTitle("Delete Order")
            .setMessage("Are you sure you want to delete this order history?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("orders").document(order.orderId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Deleted Successfully", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}