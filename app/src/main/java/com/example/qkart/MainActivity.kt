package com.example.qkart

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.cloudinary.android.MediaManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    // Firebase instances initialization
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Full Screen Setup
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)

        FirebaseMessaging.getInstance().subscribeToTopic("admin_orders")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) Log.d("FCM", "Subscribed to Admin Orders")
            }

        // 2. Cloudinary safe initialization
        initCloudinary()

        // 3. Splash Screen Timer
        Handler(Looper.getMainLooper()).postDelayed({
            goToNextScreen()
        }, 3000)
    }

    private fun initCloudinary() {
        try {
            val config = mapOf(
                "cloud_name" to "db2cwflo3"
            )
            MediaManager.init(this, config)
            Log.d("CLOUDINARY", "Cloudinary Initialized Successfully")
        } catch (e: Exception) {
            Log.e("CLOUDINARY", "MediaManager already initialized: ${e.message}")
        }
    }

    private fun goToNextScreen() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            // Case 1: Agar koi logged in nahi hai, toh seedha signupActivity
            startActivity(Intent(this, signupActivity::class.java))
            finish()
        } else {
            // Case 2: Agar user logged in hai, toh role check karo
            val uid = currentUser.uid

            // Sabse pehle "admins" collection check karo
            db.collection("admins").document(uid).get()
                .addOnSuccessListener { adminDoc ->
                    if (adminDoc.exists()) {
                        // Ye UID admin ki hai!
                        val isShopLoggedIn = adminDoc.getBoolean("isShopLoggedIn") ?: false

                        if (isShopLoggedIn) {
                            // Agar shop details filled hain
                            startActivity(Intent(this, ShopAdminHubActivity::class.java))
                        } else {
                            // Agar shop details nahi hain
                            startActivity(Intent(this, AdminDashboardActivity::class.java))
                        }
                    } else {
                        // Admin nahi hai, toh User Home par
                        startActivity(Intent(this, HomeActivity::class.java))
                    }
                    finish()
                }
                .addOnFailureListener {

                    startActivity(Intent(this, signupActivity::class.java))
                    finish()
                }
        }
    }
}