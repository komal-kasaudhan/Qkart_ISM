package com.example.qkart

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.qkart.MenuItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange

class HomeActivity : AppCompatActivity() {
    private var allShopsList = mutableListOf<Shop>()
    private val db = FirebaseFirestore.getInstance()

    // 1. Room Database Setup
    private lateinit var database: AppDatabase
    private lateinit var shopDao: ShopDao

    private lateinit var recyclerViewShops: RecyclerView
    private lateinit var shopAdapter: ShopAdapter
    private lateinit var imgProfile: ImageView
    private lateinit var imgNotification: ImageView
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Window Insets (Padding for Status Bar)

        // 2. Room DB Initialize
        database = AppDatabase.getDatabase(this)
        shopDao = database.shopDao()

        initViews()
        setUpRecyclerView()

        // 3. Pehle Room (Offline) se data load
        loadShopsFromRoom()
        listenForStatusUpdates()
        // 4. Background mein Firebase se naya data sync
        syncShopsWithFirebase()

        setupFeatureListeners()
        setupBottomNavigation()
        loadUserProfileImage()

        val imgprof = findViewById<ImageView>(R.id.img_profile)

        imgprof.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }

        // Search Logic
        val etSearch = findViewById<EditText>(R.id.et_search)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterShops(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
    private fun listenForStatusUpdates() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("user_notifications")
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshots, _ ->
                snapshots?.documentChanges?.forEach { dc ->
                    if (dc.type == DocumentChange.Type.ADDED) {
                        val title = dc.document.getString("title") ?: "Order Update"
                        val msg = dc.document.getString("message") ?: ""

                        // 🔔 System notification
                        showLocalNotification(title, msg)

                        // Mark as read taaki dobara na baje
                        dc.document.reference.update("isRead", true)
                    }
                }
            }
    }
    // 2. Local Notification Builder
    private fun showLocalNotification(title: String, message: String) {
        val channelId = "user_orders_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId, "Order Updates", android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.account_circle) // Aapka logo/icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    // Step A: Room DB se data instantly load
    private fun loadShopsFromRoom() {
        lifecycleScope.launch {

            val cachedShops = withContext(Dispatchers.IO) {
                shopDao.getAllShops()
            }
            if (cachedShops.isNotEmpty()) {
                allShopsList.clear()
                allShopsList.addAll(cachedShops)
                shopAdapter.updateList(allShopsList)
            }
        }
    }

    // Step B: Firebase se data fetch karke Room mein update karna


    private fun syncShopsWithFirebase() {
        // .get() ko hata kar .addSnapshotListener lagaya hai real-time updates ke liye
        db.collection("shops")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("HomeActivity", "Firebase Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val shopsList = mutableListOf<Shop>()
                    for (document in snapshots) {
                        // Document ID map karna zaruri hai taaki details sahi se khulein
                        val shop = document.toObject(Shop::class.java).copy(id = document.id)
                        shopsList.add(shop)
                    }

                    if (shopsList.isNotEmpty()) {
                        // 1. Memory list update karein
                        allShopsList.clear()
                        allShopsList.addAll(shopsList)

                        // 2. RecyclerView ko turant refresh karein (Isse Open/Closed status real-time dikhega)
                        shopAdapter.updateList(allShopsList)

                        // 3. Room Database (Offline Cache) ko background mein sync karein
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                shopDao.deleteAllShops() // Purana data clear
                                shopDao.insertShops(shopsList) // Naya real-time data insert
                                Log.d("HomeActivity", "Room DB Updated with Real-time Data")
                            } catch (exception: Exception) {
                                Log.e("HomeActivity", "Room Sync Error: ${exception.message}")
                            }
                        }
                    }
                }
            }
    }
    private fun filterShops(query: String) {
        val filteredList = allShopsList.filter {
            it.name.contains(query, ignoreCase = true)
        }
        shopAdapter.updateList(filteredList)
    }

    private fun initViews() {
        recyclerViewShops = findViewById(R.id.rv_shops)
        imgProfile = findViewById(R.id.img_profile)
        imgNotification = findViewById(R.id.notification)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
    }

    private fun setUpRecyclerView() {
        recyclerViewShops.layoutManager = LinearLayoutManager(this)
        shopAdapter = ShopAdapter(emptyList())
        recyclerViewShops.adapter = shopAdapter
    }

    private fun setupFeatureListeners() {
        imgProfile.setOnClickListener { Toast.makeText(this, "Profile...", Toast.LENGTH_SHORT).show() }
        imgNotification.setOnClickListener { Toast.makeText(this, "Notifications...", Toast.LENGTH_SHORT).show() }
    }
    private fun loadUserProfileImage() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
          val imgProfile = findViewById<ImageView>(R.id.img_profile)
        // addSnapshotListener ka fayda ye hai ki agar user profile photo badlega
        // toh home screen par bina refresh kiye apne aap badal jayegi
        db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("HomeActivity", "Error fetching profile: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val imageUrl = snapshot.getString("profileImage")

                if (!imageUrl.isNullOrEmpty()) {
                    // Glide se photo dikhana
                    Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.account_circle) // Jab tak load ho rahi ho
                        .error(R.drawable.account_circle)       // Agar error aaye toh
                        .circleCrop()                           // Gol cut karne ke liye
                        .into(imgProfile)
                } else {
                    // Agar photo nahi hai toh default icon dikhao
                    imgProfile.setImageResource(R.drawable.account_circle)
                }
            }
        }
    }


    private fun setupBottomNavigation() {
        // 1. View ko reference karein (Aapne binding use ki hogi toh binding.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Home pe hai toh kuch karne ki zaroorat nahi, bas true return karein
                    true
                }

                R.id.nav_orders -> {
                    val intent = Intent(this, OrderHistoryActivity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.nav_coins -> {
                    // 🌟 Yahan se EarnXPActivity call hogi
                    val intent = Intent(this, EarnXPActivity::class.java)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }
    }


    }


