package com.example.qkart

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.qkart.databinding.LayoutItemOrderAdminBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminOrdersAdapter(
    private val onStatusUpdate: (OrderModel, String) -> Unit
) : RecyclerView.Adapter<AdminOrdersAdapter.OrderViewHolder>() {

    private var orders = listOf<OrderModel>()

    fun submitList(newList: List<OrderModel>) {
        orders = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = LayoutItemOrderAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.bind(order)
    }

    override fun getItemCount() = orders.size

    inner class OrderViewHolder(private val binding: LayoutItemOrderAdminBinding) : RecyclerView.ViewHolder(binding.root) {

        private fun formatTime(time: Long): String {
            return try {
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                sdf.format(Date(time))
            } catch (e: Exception) { "N/A" }
        }

        fun bind(order: OrderModel) {
            val context = binding.root.context

            // 1. ORDER ID FIX
            val shortId = if (order.orderId.length > 4) order.orderId.takeLast(4) else order.orderId
            binding.tvOrderId.text = "Order #$shortId"

            // 2. DYNAMIC "PLACED AT" TIME
            binding.tvOrderPlacedAt.text = "Placed at: ${formatTime(order.timestamp)}"

            // 3. DELIVERY TIMING & STATUS (Old ID)
            binding.tvOrderStatus.text = order.status.uppercase()

            // 4. CUSTOMER INFO & REVENUE
            binding.tvCustomerName.text = "Customer: ${order.userName}"
            binding.tvFullAddress.text = "Address: ${order.userHostel}, Room ${order.userRoom}"

            val displayPrice = order.getTotalAmountValue()
            binding.tvTotalPaid.text = "Total Paid: ${String.format("%.2f", displayPrice)}"

            binding.tvAllPhones.text = "📞 ${order.userPhone}"

            // 5. CALL LOGIC (Old IDs)
            binding.tvAllPhones.setOnClickListener { makePhoneCall(context, order.userPhone) }
            try {
                binding.btnCallPrimary.setOnClickListener { makePhoneCall(context, order.userPhone) }
            } catch (e: Exception) {}

            // 6. ITEMS LIST
            binding.layoutItemsContainer.removeAllViews()
            order.items.forEach { item ->
                val row = LayoutInflater.from(context).inflate(R.layout.item_order_product_row, binding.layoutItemsContainer, false)
                row.findViewById<TextView>(R.id.tvRowItemName).text = item.name
                row.findViewById<TextView>(R.id.tvRowItemQty).text = "x ${item.quantity}"
                binding.layoutItemsContainer.addView(row)
            }

            // 7. EXPAND DETAILS
            binding.btnSeeDetails.setOnClickListener {
                if (binding.layoutExpandableDetails.visibility == View.GONE) {
                    binding.layoutExpandableDetails.visibility = View.VISIBLE
                    binding.btnSeeDetails.text = "Hide Details ▲"
                } else {
                    binding.layoutExpandableDetails.visibility = View.GONE
                    binding.btnSeeDetails.text = "See Delivery Details ▼"
                }
            }

            // 8. STATUS UPDATE
            binding.btnUpdateStatus.setOnClickListener {
                showStatusSelectionDialog(context, order)
            }
        }

        private fun makePhoneCall(context: Context, number: String?) {
            if (!number.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$number")
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show()
            }
        }

        private fun showStatusSelectionDialog(context: Context, order: OrderModel) {
            val options = arrayOf("Accepted", "Preparing", "Ready", "Delivered", "Rejected", "Set Custom Time")
            AlertDialog.Builder(context)
                .setTitle("Update Status (#${order.orderId.takeLast(4)})")
                .setSingleChoiceItems(options, -1) { dialog, which ->
                    val selectedOption = options[which]
                    dialog.dismiss()

                    // 🌟 FIX: Callback Activity ko bheja ja raha hai bina local picker kholi
                    // Activity khud decide karegi ki TimePicker dikhana hai ya update karna hai
                    if (selectedOption == "Set Custom Time") {
                        onStatusUpdate(order, "Set Custom Time")
                    } else {
                        showConfirmationDialog(context, order, selectedOption)
                    }
                }
                .show()
        }

        private fun showConfirmationDialog(context: Context, order: OrderModel, newStatus: String) {
            AlertDialog.Builder(context)
                .setTitle("Confirm Update")
                .setMessage("Mark order as '$newStatus'?")
                .setPositiveButton("Yes") { _, _ ->
                    onStatusUpdate(order, newStatus)
                }
                .setNegativeButton("No", null)
                .show()
        }
    }
}