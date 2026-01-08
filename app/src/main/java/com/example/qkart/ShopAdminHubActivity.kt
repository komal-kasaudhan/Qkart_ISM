package com.example.qkart

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.qkart.databinding.ActivityShopAdminHubBinding

class ShopAdminHubActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShopAdminHubBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopAdminHubBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Status Bar Margin
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 2. Fetch Shop Data from SharedPreferences
        val sharedPref = getSharedPreferences("AdminPrefs", MODE_PRIVATE)
        val shopId = sharedPref.getString("SAVED_SHOP_ID", null)
        val shopName = sharedPref.getString("SAVED_SHOP_NAME", "Admin Hub")

        // Shop Name UI par set karo
        binding.tvHubShopName.text = shopName

        // 3. Navigation Logic

        // --- Live Orders Click ---
        binding.cardLiveOrders.setOnClickListener {
            val intent = Intent(this, AdminOrdersActivity::class.java)
            // Shop ID pass karna zaroori hai taaki sirf iss shop ke orders dikhein
            intent.putExtra("SHOP_ID", shopId)
            startActivity(intent)
        }

        // --- Manage Menu Page Click ---
        binding.cardManageMenu.setOnClickListener {

            val intent = Intent(this, AdminMenuActivity::class.java)
            intent.putExtra("SHOP_ID", shopId)
            intent.putExtra("SHOP_NAME", shopName)
            startActivity(intent)
        }

        // --- Discount Coupons Click ---
        binding.cardManageCoupons.setOnClickListener {

            val intent = Intent(this, ManageCouponsActivity::class.java)
            intent.putExtra("SHOP_ID", shopId)
            startActivity(intent)
        }

        // 4. Switch / Logout Shop Logic (Green Button)
        binding.btnSwitchShop.setOnClickListener {

            sharedPref.edit().clear().apply()

            Toast.makeText(this, "Logged out from $shopName", Toast.LENGTH_SHORT).show()


            val intent = Intent(this, AdminDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}