

package com.example.qkart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CouponAdapter(
    private var couponList: List<Coupon>,
    private val onDeleteClick: (Coupon) -> Unit
) : RecyclerView.Adapter<CouponAdapter.CouponViewHolder>() {


    class CouponViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCode: TextView = view.findViewById(R.id.tvCouponCode)
        val tvDesc: TextView = view.findViewById(R.id.tvCouponDesc)
        val tvMinOrder: TextView = view.findViewById(R.id.tvCouponMinOrder)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteCoupon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CouponViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_coupon_layout, parent, false)
        return CouponViewHolder(view)
    }

    override fun onBindViewHolder(holder: CouponViewHolder, position: Int) {
        val coupon = couponList[position]

        // Data Set Karna
        holder.tvCode.text = coupon.code
        holder.tvDesc.text = "Discount: ₹${coupon.discountPrice} OFF"
        holder.tvMinOrder.text = "Min Order: ₹${coupon.minOrderValue} | Limit: ${coupon.maxUsageLimit} users"

        // Delete Button ka logic
        holder.btnDelete.setOnClickListener {
            onDeleteClick(coupon)
        }
    }

    override fun getItemCount(): Int = couponList.size


    fun updateList(newList: List<Coupon>) {
        couponList = newList
        notifyDataSetChanged()
    }
}