package com.example.qkart

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.qkart.databinding.ActivityEarnXpactivityBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EarnXPActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEarnXpactivityBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEarnXpactivityBinding.inflate(layoutInflater)
        setContentView(binding.root)


        observeUserXP()
    }

    private fun observeUserXP() {
        val uid = auth.currentUser?.uid ?: return


        db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    // Firestore se current XP fetch karna
                    val totalXP = snapshot.getLong("totalXP")?.toInt() ?: 0

                    // 1. UI update: Total XP aur Cash Value
                    binding.tvTotalXP.text = "$totalXP XP"
                    binding.tvCashValue.text = "Value: ₹${totalXP / 10}.00"

                    // 2. Diamond Membership Logic (Goal: 1000 XP)
                    updateDiamondProgress(totalXP)
                }
            }
    }

    private fun updateDiamondProgress(currentXP: Int) {
        val goal = 1000

        // Progress bar update karna (0 to 1000)
        binding.pbDiamond.progress = if (currentXP <= goal) currentXP else goal

        if (currentXP >= goal) {
            // 💎 Diamond Member banne par UI change
            binding.tvStatus.text = "CONGRATS! YOU ARE A DIAMOND MEMBER 💎"
            binding.tvStatus.setTextColor(Color.parseColor("#00B0FF")) // Bright Blue
            binding.pbDiamond.progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#00B0FF"))
        } else {
            // Target dikhana
            val remaining = goal - currentXP
            binding.tvStatus.text = "Only $remaining XP away from Diamond Perks! 🚀"
            binding.tvStatus.setTextColor(Color.GRAY)
        }
    }
}