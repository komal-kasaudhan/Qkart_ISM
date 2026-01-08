package com.example.qkart

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.qkart.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

private fun loginActivity.checkUserType(email: String) {}

class loginActivity : AppCompatActivity() {
    lateinit var binding : ActivityLoginBinding
    lateinit var auth : FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        binding.btnLogin.setOnClickListener {
            val email = binding.etLoginEmail.text.toString().trim()
            val pass = binding.etLoginPassword.text.toString().trim()
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    checkUserType(email)
                } else {
                    Toast.makeText(
                        this,
                        "Login Failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }
        binding.tvSignupRedirect.setOnClickListener {
            val intent = Intent(this, signupActivity::class.java)
            startActivity(intent)
            finish()

        }

    }
        private fun checkUserType(email: String) {
            if (email == "admin@qkart.com") {
                // Agar Admin hai -> Admin Dashboard
                Toast.makeText(this, "Welcome Admin!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, AdminDashboardActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                // Agar Normal User hai -> Main Home Page
                Toast.makeText(this, "Welcome User!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
