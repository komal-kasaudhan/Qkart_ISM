package com.example.qkart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AdminShopAdapter(
    private var shopsList: List<Shop>,
    private val onEditClick: (Shop) -> Unit, // 🌟 Naya Edit Callback
    private val onShopClick: (Shop) -> Unit
) : RecyclerView.Adapter<AdminShopAdapter.ShopViewHolder>() {

    class ShopViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_admin_shop_name)
        val tvRating: TextView = view.findViewById(R.id.tv_admin_shop_rating)
        val switchStatus: SwitchCompat = view.findViewById(R.id.switch_shop_status)
        val imgShop: ImageView = view.findViewById(R.id.img_admin_shop_icon)


        val btnEdit: ImageView = view.findViewById(R.id.img_admin_manage_menu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop_admin_row, parent, false)
        return ShopViewHolder(view)
    }


    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val shop = shopsList[position]
        val context = holder.itemView.context

        holder.tvName.text = shop.name
        holder.tvRating.text = "${shop.rating} ★"

        // --- 1. Reset & Set initial status ---
        holder.switchStatus.setOnCheckedChangeListener(null)
        holder.switchStatus.isChecked = shop.isAvailable

        // --- 2. Color & Text Visual Logic ---
        fun updateSwitchVisuals(isChecked: Boolean) {
            if (isChecked) {
                holder.switchStatus.text = "OPEN"
                holder.switchStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32")) // Green
            } else {
                holder.switchStatus.text = "CLOSED"
                holder.switchStatus.setTextColor(android.graphics.Color.parseColor("#D32F2F")) // Red
            }
        }
        updateSwitchVisuals(shop.isAvailable)

        // --- 3. Image Loading ---
        Glide.with(context)
            .load(shop.image)
            .placeholder(R.drawable.add_a_photo)
            .centerCrop()
            .into(holder.imgShop)

        // --- 4. Switch Toggle with Firestore Update ---
        holder.switchStatus.setOnCheckedChangeListener { _, isChecked ->
            updateSwitchVisuals(isChecked) // Turant UI badlo

            // Firebase Firestore update logic
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("shops")
                .document(shop.id)
                .update("isAvailable", isChecked)
                .addOnFailureListener {

                    holder.switchStatus.isChecked = !isChecked
                    android.widget.Toast.makeText(context, "Update failed!", android.widget.Toast.LENGTH_SHORT).show()
                }
        }

        // --- 5. Click Listeners ---
        holder.btnEdit.setOnClickListener { onEditClick(shop) }
        holder.itemView.setOnClickListener { onShopClick(shop) }
    }
    override fun getItemCount(): Int = shopsList.size

    fun updateList(newList: List<Shop>) {
        shopsList = newList
        notifyDataSetChanged()
    }
}