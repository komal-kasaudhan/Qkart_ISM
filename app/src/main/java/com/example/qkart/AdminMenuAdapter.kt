package com.example.qkart

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class AdminMenuAdapter(
    private var menuList: List<MenuItem>,
    private val shopId: String,
    private var currentCategory: String = "menu",
    private val onEditClick: (MenuItem) -> Unit,
    private val onDeleteClick: (String, String) -> Unit
) : RecyclerView.Adapter<AdminMenuAdapter.MenuViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    class MenuViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val imgItem: ImageView = view.findViewById(R.id.img_item_admin)
        val tvName: TextView = view.findViewById(R.id.tv_item_name_admin)
        val tvPrice: TextView = view.findViewById(R.id.tv_item_price_admin)
        val tvDesc: TextView = view.findViewById(R.id.tv_item_desc_admin)
        val tvOffer: TextView = view.findViewById(R.id.tv_item_offer_admin)
        val switchStatus: SwitchCompat = view.findViewById(R.id.switch_item_status)
        val btnDelete: ImageView = view.findViewById(R.id.btn_delete_item)
        val btnEdit: ImageView = view.findViewById(R.id.btn_edit_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_admin_row, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item = menuList[position]


        holder.tvName.text = item.name
        holder.tvPrice.text = "₹${item.price}"
        holder.tvDesc.text = item.description

        // Professional Green Theme
        holder.tvPrice.setTextColor(Color.parseColor("#4CAF50"))

        if (item.offer.isEmpty()) {
            holder.tvOffer.visibility = View.GONE
        } else {
            holder.tvOffer.visibility = View.VISIBLE
            holder.tvOffer.text = item.offer
        }

        Glide.with(holder.itemView.context)
            .load(item.image)
            .centerCrop()
            .placeholder(R.drawable.add_a_photo)
            .into(holder.imgItem)

        // 2. Stock Status Toggle (Dynamic Path Logic)
        holder.switchStatus.setOnCheckedChangeListener(null)
        holder.switchStatus.isChecked = item.isAvailable
        holder.switchStatus.thumbTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))

        holder.switchStatus.setOnCheckedChangeListener { _, isChecked ->
            // 🌟 Yahan magic hai: currentCategory batayega ki 'menu' mein update karna hai ya 'special_combos' mein
            db.collection("shops").document(shopId)
                .collection(currentCategory)
                .document(item.id)
                .update("isAvailable", isChecked)
                .addOnSuccessListener {
                    val msg = if (isChecked) "In Stock" else "Out of Stock"
                    Toast.makeText(holder.itemView.context, "$msg in $currentCategory", Toast.LENGTH_SHORT).show()
                }
        }


        holder.btnEdit.setOnClickListener {

            onEditClick(item)
        }

        holder.btnDelete.setOnClickListener {
            // Delete function ko ID aur Category dono bhej rahe hain
            onDeleteClick(item.id, currentCategory)
        }
    }

    override fun getItemCount(): Int = menuList.size

    fun updateList(newList: List<MenuItem>, category: String) {
        menuList = newList
        currentCategory = category
        notifyDataSetChanged()
    }
}