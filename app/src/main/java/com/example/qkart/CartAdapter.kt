package com.example.qkart

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.qkart.databinding.CartitemBinding

class CartAdapter(
    private val onQuantityChanged: (CartItem) -> Unit,
    private val onDeleteRequested: (CartItem, Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private var items = mutableListOf<CartItem>()

    fun submitList(newList: List<CartItem>) {
        items = newList.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = CartitemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position)
    }

    override fun getItemCount(): Int = items.size

    inner class CartViewHolder(private val binding: CartitemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItem, position: Int) {
            binding.tvCartItemName.text = item.name
            binding.tvQtyCart.text = item.quantity.toString()

            // 🌟 PRICE LOGIC
            val totalDiscountedPrice = item.discountedPrice * item.quantity
            val totalOriginalPrice = item.originalPrice * item.quantity

            // Discounted Price
            binding.tvCartItemPrice.text = "₹${totalDiscountedPrice.toInt()}"

            // 🌟 OPTIONAL: Agar aapke XML mein purani price (tvOriginalPrice) hai toh:
            // binding.tvOriginalPrice.text = "₹${totalOriginalPrice.toInt()}"
            // binding.tvOriginalPrice.paintFlags = binding.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            // Image loading
            Glide.with(binding.root.context)
                .load(item.image)
                .placeholder(R.drawable.add_a_photo) // Ek default image rakhein
                .into(binding.ivCartItemImg)

            // ➕ PLUS Button
            binding.btnPlusCart.setOnClickListener {
                item.quantity++
                onQuantityChanged(item)
                notifyItemChanged(position)
            }

            // ➖ MINUS Button
            binding.btnMinusCart.setOnClickListener {
                if (item.quantity > 1) {
                    item.quantity--
                    onQuantityChanged(item)
                    notifyItemChanged(position)
                } else {
                    onDeleteRequested(item, position)
                }
            }
        }
    }
}