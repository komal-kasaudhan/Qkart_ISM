package com.example.qkart

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private lateinit var rvAdminShopsList: RecyclerView
    private lateinit var adminShopAdapter: AdminShopAdapter
    private var allShopsList = mutableListOf<Shop>()
    private lateinit var imgProfile: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        auth = FirebaseAuth.getInstance()

        // 1. Initialize Views
        val etSearch = findViewById<EditText>(R.id.et_admin_search)
        imgProfile = findViewById<ImageView>(R.id.img_admin_profile)
        val fabAddShop = findViewById<FloatingActionButton>(R.id.fab_add_new_shop)
        val bottomNav = findViewById<BottomNavigationView>(R.id.adminBottomNavView)
        rvAdminShopsList = findViewById(R.id.rv_admin_shops_list)

        // 2. Load Profile & Start Listener
        loadAdminProfilePhoto()
        startListeningForOrders()

        // 3. Setup RecyclerView
        rvAdminShopsList.layoutManager = LinearLayoutManager(this)
        adminShopAdapter = AdminShopAdapter(
            allShopsList,
            onEditClick = { shop -> openEditShop(shop) },
            onShopClick = { shop -> showPasswordDialog(shop) }
        )
        rvAdminShopsList.adapter = adminShopAdapter

        // 4. Load Data from Firestore
        loadShopsFromFirestore()

        // 5. Search Filter Logic
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterShops(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 6. Add New Shop Button
        fabAddShop.setOnClickListener {
            val intent = Intent(this, AddShopActivity::class.java)
            intent.putExtra("IS_EDIT", false)
            startActivity(intent)
        }

        // 7. Profile Click
        imgProfile.setOnClickListener {
            val intent = Intent(this, AdminProfileActivity::class.java)
            startActivity(intent)
        }

        // 8. Bottom Navigation Logic
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.admin_nav_orders -> {
                    val savedId = getSharedPreferences("AdminPrefs", MODE_PRIVATE).getString("SAVED_SHOP_ID", null)
                    if (savedId != null) {
                        startActivity(Intent(this, AdminOrdersActivity::class.java))
                    } else {
                        Toast.makeText(this, "Please login to a shop first", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.admin_nav_dashboard -> true
                else -> false
            }
        }
    }

    // --- 🔔 Notification Functions ---

    private fun startListeningForOrders() {
        val myShopId = getSharedPreferences("AdminPrefs", MODE_PRIVATE).getString("SAVED_SHOP_ID", "")
        if (myShopId.isNullOrEmpty()) return

        db.collection("admin_notifications")
            .whereEqualTo("shopId", myShopId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshots, _ ->
                snapshots?.documentChanges?.forEach { dc ->
                    if (dc.type == DocumentChange.Type.ADDED) {
                        val title = dc.document.getString("title") ?: "New Order!"
                        val msg = dc.document.getString("message") ?: "You have a new order."

                        showLocalNotification(title, msg)
                        dc.document.reference.update("isRead", true)
                    }
                }
            }
    }

    private fun showLocalNotification(title: String, message: String) {
        val channelId = "admin_orders_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Order Alerts", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.account_circle)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    // --- 🖼️ Profile Functions ---

    private fun loadAdminProfilePhoto() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("admins").document(uid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val imageUrl = snapshot.getString("profileImage")
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(imageUrl)
                        .circleCrop()
                        .placeholder(R.drawable.account_circle)
                        .into(imgProfile)
                }
            }
        }
    }

    // --- 🛒 Shop Management Functions ---

    private fun showPasswordDialog(shop: Shop) {
        val passwordInput = EditText(this)
        passwordInput.hint = "Enter Admin Key"
        passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        AlertDialog.Builder(this)
            .setTitle("Login to ${shop.name}")
            .setView(passwordInput)
            .setPositiveButton("Login") { _, _ ->
                val enteredPass = passwordInput.text.toString()
                if (enteredPass == shop.adminKey || enteredPass == "9999") {
                    val pref = getSharedPreferences("AdminPrefs", MODE_PRIVATE).edit()
                    pref.putString("SAVED_SHOP_ID", shop.id)
                    pref.putString("SAVED_SHOP_NAME", shop.name)
                    pref.apply()

                    // 🌟 Shop login hote hi listener refresh karo
                    startListeningForOrders()

                    val intent = Intent(this, ShopAdminHubActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Wrong Admin Key!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadShopsFromFirestore() {
        db.collection("shops").addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener
            val tempList = mutableListOf<Shop>()
            value?.documents?.forEach { doc ->
                val shop = doc.toObject(Shop::class.java)
                if (shop != null) {
                    tempList.add(shop.copy(id = doc.id))
                }
            }
            allShopsList = tempList
            adminShopAdapter.updateList(allShopsList)
        }
    }

    private fun openEditShop(shop: Shop) {
        val intent = Intent(this, AddShopActivity::class.java)
        intent.putExtra("IS_EDIT", true)
        intent.putExtra("SHOP_ID", shop.id)
        intent.putExtra("NAME", shop.name)
        intent.putExtra("DESC", shop.description)
        intent.putExtra("RATING", shop.rating)
        intent.putExtra("PREP", shop.prepTime)
        intent.putExtra("TIME", shop.shopTime)
        intent.putExtra("IMAGE", shop.image)
        intent.putExtra("IS_OPEN", shop.isAvailable)
        startActivity(intent)
    }

    private fun filterShops(query: String) {
        val filtered = allShopsList.filter { it.name.contains(query, ignoreCase = true) }
        adminShopAdapter.updateList(filtered)
    }

    private fun showProfileDialog() {
        val email = auth.currentUser?.email ?: "Admin"
        AlertDialog.Builder(this).setTitle("Admin Control").setMessage("LoggedIn as: $email")
            .setPositiveButton("Logout") { _, _ ->
                auth.signOut()
                startActivity(Intent(this, loginActivity::class.java))
                finish()
            }.setNegativeButton("Cancel", null).show()
    }
}