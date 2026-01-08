package com.example.qkart

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.qkart.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class signupActivity : AppCompatActivity() {
    lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance() // 1. Firestore ka instance liya

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnSignup.setOnClickListener {

                Toast.makeText(this, "Button Clicked!", Toast.LENGTH_SHORT).show()
                // baki code...

            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()
            val name = binding.etName.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid

                        // 2. User/Admin ka data Map mein taiyar karna
                        val userData = hashMapOf(
                            "uid" to uid,
                            "name" to name,
                            "email" to email,
                            "role" to if (email == "admin@qkart.com") "admin" else "user",
                            "profileImage" to "",
                            "phoneNumber" to ""
                        )

                        // 3. Firestore mein save (admins ya users collection mein)
                        val collectionName = if (email == "admin@qkart.com") "admins" else "users"

                        if (uid != null) {
                            db.collection(collectionName).document(uid)
                                .set(userData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Profile Saved in Database!", Toast.LENGTH_SHORT).show()

                                    // Screen change logic
                                    if (email == "admin@qkart.com") {
                                        startActivity(Intent(this, AdminDashboardActivity::class.java))
                                    } else {
                                        startActivity(Intent(this, MainActivity::class.java)) // Ya HomeActivity
                                    }
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Firestore Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Auth Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        binding.tvLoginRedirect.setOnClickListener {
            startActivity(Intent(this, loginActivity::class.java))
            finish()
        }
    }
}