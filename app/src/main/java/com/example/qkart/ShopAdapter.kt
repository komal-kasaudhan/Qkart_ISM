// File: app/src/main/java/com/example/qkart/ShopAdapter.kt

package com.example.qkart
import com.example.qkart.Shop
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Image loading library ke liye (zaroori)

// Assumed Shop data class exists:
/*
data class Shop(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val rating: Double = 0.0,
    val isAvailable: Boolean = true // Shop OPEN/CLOSED status
)
*/



class ShopAdapter(private var shopsList: List<Shop>) :
    RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {


        // 1. updateList function ko alag se yahan rakhein
        fun updateList(newList: List<Shop>) {
            this.shopsList = newList
            notifyDataSetChanged()
        }



    // Inner class jo views ko hold karta hai (jaise FindViewById)
    class ShopViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val shopImage: ImageView = view.findViewById(R.id.img_shop_photo)
        val shopName: TextView = view.findViewById(R.id.tv_shop_name)
        val shopDescription: TextView = view.findViewById(R.id.tv_shop_description)
        val shopRating: TextView = view.findViewById(R.id.tv_rating)
        val shopStatus: TextView = view.findViewById(R.id.tv_shop_status)
    }

    // 1. Layout ko inflate karta hai (Layout file se design banata hai)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_row, parent, false)
        return ShopViewHolder(view)

    }





    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val shop = shopsList[position]
        val context = holder.itemView.context

        holder.shopName.text = shop.name
        holder.shopDescription.text = shop.description
        holder.shopRating.text = shop.rating.toString()

        Glide.with(context)
            .load(shop.image)
            .placeholder(R.drawable.add_a_photo)
            .into(holder.shopImage)

        // --- MISSING LOGIC 1: Visual Feedback (Alpha & Grayscale) ---
        if (shop.isAvailable) {
            holder.shopStatus.text = "OPEN"
            holder.shopStatus.background = ContextCompat.getDrawable(context, R.drawable.round_shape_green)
            holder.itemView.alpha = 1.0f
        } else {
            holder.shopStatus.text = "CLOSED"
            holder.shopStatus.background = ContextCompat.getDrawable(context, R.drawable.round_shape_red)
            holder.itemView.alpha = 0.6f
        }

        // --- MISSING LOGIC 2: Intent Blocking ---
        holder.itemView.setOnClickListener {
            if (shop.isAvailable) {
                val intent = Intent(context, MenuActivity::class.java)
                intent.putExtra("SHOP_ID", shop.id)
                intent.putExtra("SHOP_NAME", shop.name)
                intent.putExtra("SHOP_IMAGE", shop.image)
                intent.putExtra("SHOP_RATING", shop.rating)
                intent.putExtra("SHOP_DESC", shop.description)
                intent.putExtra("SHOP_TIME", shop.shopTime)
                intent.putExtra("PREP_TIME", shop.prepTime)
                // Extra safety: shop status bhi bhej do
                intent.putExtra("IS_AVAILABLE", shop.isAvailable)
                context.startActivity(intent)
            } else {

                android.widget.Toast.makeText(context,
                    "Currently not accepting orders.Please check back later!",
                    android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 3. List mein kitne items hain
    override fun getItemCount() = shopsList.size

}