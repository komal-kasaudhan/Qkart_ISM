package com.example.qkart

import android.graphics.Paint
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.qkart.databinding.UserMenuRowBinding

class UserMenuAdapter(
    private val onCartChanged: (MenuItem, Int) -> Unit
) : RecyclerView.Adapter<UserMenuAdapter.MenuViewHolder>() {

    private var fullList = listOf<MenuItem>()
    private var displayList = mutableListOf<MenuItem>()

    class MenuViewHolder(val binding: UserMenuRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = UserMenuRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item = displayList[position]

        // 🌟 Discount calculate karo
        val finalCalculatedPrice = item.calculateDiscountedPriceFromOffer()

        holder.binding.tvItemName.text = item.name
        holder.binding.tvItemRating.text = "${item.rating} ★"

        // 🌟 PRICE LOGIC UPDATE: Yahan se "Ganda" look sahi hoga
        if (finalCalculatedPrice < item.price) {
            // CASE 1: Agar Discount hai toh dono dikhao
            holder.binding.tvDiscountedPrice.text = "₹${finalCalculatedPrice.toInt()}"

            holder.binding.tvItemPrice.text = "₹${item.price.toInt()}"
            holder.binding.tvItemPrice.paintFlags = holder.binding.tvItemPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.binding.tvItemPrice.visibility = View.VISIBLE // Cut price show karo
        } else {
            // CASE 2: Agar Discount NAHI hai toh sirf ek price dikhao
            holder.binding.tvDiscountedPrice.text = "₹${item.price.toInt()}"
            holder.binding.tvItemPrice.visibility = View.GONE // Cut price hide kar do
        }

        // Description Logic (Same as before)
        holder.binding.tvDescription.text = item.description
        holder.binding.tvDescription.maxLines = 2
        holder.binding.tvDescription.ellipsize = TextUtils.TruncateAt.END

        holder.binding.tvReadMore.setOnClickListener {
            if (holder.binding.tvDescription.maxLines == 2) {
                holder.binding.tvDescription.maxLines = Int.MAX_VALUE
                holder.binding.tvDescription.ellipsize = null
            } else {
                holder.binding.tvDescription.maxLines = 2
                holder.binding.tvDescription.ellipsize = TextUtils.TruncateAt.END
            }
        }

        // Offer Tag Logic
        if (item.offer.isNotEmpty()) {
            holder.binding.tvSpecialOffer.visibility = View.VISIBLE
            holder.binding.tvSpecialOffer.text = item.offer
        } else {
            holder.binding.tvSpecialOffer.visibility = View.GONE
        }

        Glide.with(holder.itemView.context)
            .load(item.image)
            .centerCrop()
            .placeholder(R.drawable.pizza)
            .into(holder.binding.ivItemImage)

        if (!item.isAvailable) {
            holder.binding.layoutOutOfStock.visibility = View.VISIBLE
            holder.binding.btnInitialAdd.visibility = View.GONE
            holder.binding.layoutQuantity.visibility = View.GONE
        } else {
            holder.binding.layoutOutOfStock.visibility = View.GONE
            updateQuantityUI(holder, item)
        }

        holder.binding.btnInitialAdd.setOnClickListener {
            item.localQuantity = 1
            updateQuantityUI(holder, item)
            onCartChanged(item, item.localQuantity)
        }

        holder.binding.btnAddMore.setOnClickListener {
            item.localQuantity++
            holder.binding.tvQuantityCount.text = item.localQuantity.toString()
            onCartChanged(item, item.localQuantity)
        }

        holder.binding.btnRemove.setOnClickListener {
            if (item.localQuantity > 0) {
                item.localQuantity--
                updateQuantityUI(holder, item)
                onCartChanged(item, item.localQuantity)
            }
        }
    }

    private fun updateQuantityUI(holder: MenuViewHolder, item: MenuItem) {
        if (item.localQuantity > 0) {
            holder.binding.btnInitialAdd.visibility = View.GONE
            holder.binding.layoutQuantity.visibility = View.VISIBLE
            holder.binding.tvQuantityCount.text = item.localQuantity.toString()
        } else {
            holder.binding.btnInitialAdd.visibility = View.VISIBLE
            holder.binding.layoutQuantity.visibility = View.GONE
        }
    }

    fun filter(query: String) {
        displayList = if (query.isEmpty()) {
            fullList.toMutableList()
        } else {
            fullList.filter { it.name.contains(query, ignoreCase = true) }.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun submitList(newList: List<MenuItem>) {
        val quantityMap = fullList.associate { it.id to it.localQuantity }
        newList.forEach { newItem ->
            newItem.localQuantity = quantityMap[newItem.id] ?: 0
        }
        fullList = newList
        displayList = newList.toMutableList()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = displayList.size
}