package com.example.qkart

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class OrderAdapter(
    private val context: Context,
    private var orderList: List<UserOrderModel>,
    private val onCancelClick: (UserOrderModel) -> Unit,
    private val onDeleteClick: (UserOrderModel) -> Unit,
    private val onReorderClick: (UserOrderModel) -> Unit // 🌟 Naya 5th parameter added
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    fun updateData(newList: List<UserOrderModel>) {
        this.orderList = newList
        notifyDataSetChanged()
    }

    class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val shopName: TextView = view.findViewById(R.id.tv_shop_name)
        val status: TextView = view.findViewById(R.id.tv_order_status)
        val items: TextView = view.findViewById(R.id.tv_order_items)
        val price: TextView = view.findViewById(R.id.tv_order_price)
        val date: TextView = view.findViewById(R.id.tv_order_date)
        val btnCancel: MaterialButton = view.findViewById(R.id.btn_cancel_order)
        val btnReorder: MaterialButton = view.findViewById(R.id.btn_reorder)
        val btnDelete: ImageView = view.findViewById(R.id.btn_delete_order)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_order_history, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orderList[position]

        holder.shopName.text = if (order.shopName.isEmpty()) "Unknown Shop" else order.shopName
        holder.price.text = order.getFormattedPrice()
        holder.items.text = order.getItemsAsString()

        // 🌟 STATUS TEXT LOGIC
        val s = order.status.lowercase().trim()
        if ((s == "accepted" || s == "placed" || s == "pending") &&
            order.deliveryTiming != "Instant" && order.deliveryTiming.isNotEmpty()) {
            holder.status.text = "DELIVERY AT: ${order.deliveryTiming}"
        } else {
            holder.status.text = order.status.uppercase()
        }

        // 🌟 STATUS COLOR LOGIC (Fixing the color issue)
        val statusColor = when (s) {
            "pending", "placed" -> "#2196F3"    // Blue
            "accepted" -> {
                if (order.deliveryTiming != "Instant") "#9C27B0" else "#2196F3"
            }
            "preparing" -> "#FF9800"          // Orange
            "ready" -> "#00BCD4"              // Teal/Cyan
            "delivered" -> "#4CAF50"          // Green
            "cancelled", "rejected" -> "#F44336" // Red
            else -> "#757575"                 // Gray
        }

        try {
            holder.status.backgroundTintList = ColorStateList.valueOf(Color.parseColor(statusColor))
        } catch (e: Exception) {
            holder.status.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
        }

        if (order.timestamp > 0) {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            holder.date.text = sdf.format(Date(order.timestamp))
        } else {
            holder.date.text = "N/A"
        }

        // Visibility Logic
        holder.btnCancel.visibility = if (order.canCancelOrder()) View.VISIBLE else View.GONE

        // Reorder button Past orders (Delivered, Cancelled, Rejected) par dikhega
        holder.btnReorder.visibility = if (s == "delivered" || s == "cancelled" || s == "rejected")
            View.VISIBLE else View.GONE

        // 🌟 CLICK LISTENERS
        holder.btnCancel.setOnClickListener { onCancelClick(order) }
        holder.btnDelete.setOnClickListener { onDeleteClick(order) }
        holder.btnReorder.setOnClickListener { onReorderClick(order) } // 🌟 Added listener
    }

    override fun getItemCount() = orderList.size
}