package com.example.qkart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class SpecialComboAdapter(
    // 🌟 Change 1: Callback ab (MenuItem, Int) lega Room DB connectivity ke liye
    private val onUpdate: (MenuItem, Int) -> Unit
) : RecyclerView.Adapter<SpecialComboAdapter.ViewHolder>() {

    private var list = listOf<MenuItem>()

    fun submitList(newList: List<MenuItem>) {
        list = newList
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.ivComboImg)
        val name: TextView = view.findViewById(R.id.tvComboName)
        val price: TextView = view.findViewById(R.id.tvComboPrice)

        val btnAdd: Button = view.findViewById(R.id.btnComboAdd)
        val layoutQty: LinearLayout = view.findViewById(R.id.comboStepper)
        val btnPlus: ImageView = view.findViewById(R.id.btnComboPlus)
        val btnMinus: ImageView = view.findViewById(R.id.btnComboMinus)
        val tvQty: TextView = view.findViewById(R.id.tvComboQty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_special_combo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.name.text = item.name
        holder.price.text = "₹${item.price}"

        // Glide for Images (Existing Logic)
        Glide.with(holder.itemView.context)
            .load(item.image)
            .override(400, 400)
            .placeholder(R.drawable.pizza)
            .error(R.drawable.pizza)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(holder.img)

        // 🌟 Initial UI State: Sync with item's current quantity
        if (item.localQuantity > 0) {
            holder.btnAdd.visibility = View.GONE
            holder.layoutQty.visibility = View.VISIBLE
            holder.tvQty.text = item.localQuantity.toString()
        } else {
            holder.btnAdd.visibility = View.VISIBLE
            holder.layoutQty.visibility = View.GONE
        }

        // 🌟 Add Button Logic
        holder.btnAdd.setOnClickListener {
            item.localQuantity = 1
            holder.btnAdd.visibility = View.GONE
            holder.layoutQty.visibility = View.VISIBLE
            holder.tvQty.text = "1"


            onUpdate(item, item.localQuantity)
        }

        // 🌟 Plus Button Logic
        holder.btnPlus.setOnClickListener {
            item.localQuantity++
            holder.tvQty.text = item.localQuantity.toString()

            onUpdate(item, item.localQuantity)
        }

        // 🌟 Minus Button Logic
        holder.btnMinus.setOnClickListener {
            if (item.localQuantity > 0) {
                item.localQuantity--

                if (item.localQuantity == 0) {
                    holder.layoutQty.visibility = View.GONE
                    holder.btnAdd.visibility = View.VISIBLE
                } else {
                    holder.tvQty.text = item.localQuantity.toString()
                }


                onUpdate(item, item.localQuantity)
            }
        }
    }

    override fun getItemCount() = list.size
}