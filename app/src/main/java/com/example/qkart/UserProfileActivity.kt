package com.example.qkart

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.qkart.databinding.ActivityUserProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val uid = auth.currentUser?.uid

    private var selectedImageUri: Uri? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivUserProfile.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- CLOUDINARY INITIALIZATION (RIGHT PLACE) ---
        try {
            val config = HashMap<String, String>()
            config["upload_preset"] = "zl6numjv"
            config["cloud_name"] = "db2cwflo3"
            config["api_key"] = "854955711338177"
            config["api_secret"] = "fS8L86doayXdxF6CSYYQohF7H9I"
            MediaManager.init(this, config)
        } catch (e: Exception) {
            Log.d("Cloudinary", "Already initialized or error: ${e.message}")
        }

        fetchUserData()

        binding.btnChangePhoto.setOnClickListener { imagePicker.launch("image/*") }
        binding.btnUpdateProfile.setOnClickListener { validateAndUpdate() }
        binding.btnLogout.setOnClickListener { showLogoutDialog() }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun fetchUserData() {
        if (uid == null) return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // Ye fields hamesha saved rahengi Firestore mein
                    binding.etUserName.setText(doc.getString("name"))
                    binding.tvUserEmail.text = doc.getString("email")
                    binding.etUserPhone.setText(doc.getString("phoneNumber"))
                    binding.etUserAddress.setText(doc.getString("address"))

                    val imgUrl = doc.getString("profileImage")
                    if (!imgUrl.isNullOrEmpty()) {
                        Glide.with(this).load(imgUrl).placeholder(R.drawable.person).into(binding.ivUserProfile)
                    }
                }
            }
    }

    private fun validateAndUpdate() {
        val name = binding.etUserName.text.toString().trim()
        val phone = binding.etUserPhone.text.toString().trim()
        val address = binding.etUserAddress.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Fill all the details!", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri != null) {
            uploadToCloudinary(selectedImageUri!!, name, phone, address)
        } else {
            saveToFirestore(null, name, phone, address)
        }
    }

    private fun uploadToCloudinary(uri: Uri, name: String, phone: String, address: String) {
        val pd = ProgressDialog(this)
        pd.setMessage("Uploading Profile...")
        pd.setCancelable(false)
        pd.show()

        // Unsigned upload preset check karna Cloudinary dashboard mein
        MediaManager.get().upload(uri).unsigned("zl6numjv").callback(object : UploadCallback {
            override fun onStart(requestId: String?) {}
            override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

            override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                val imageUrl = resultData?.get("secure_url").toString()
                saveToFirestore(imageUrl, name, phone, address)
                pd.dismiss()
            }

            override fun onError(requestId: String?, error: ErrorInfo?) {
                pd.dismiss()
                Log.e("CloudinaryError", error?.description ?: "Unknown error")
                Toast.makeText(this@UserProfileActivity, "Error: ${error?.description}", Toast.LENGTH_SHORT).show()
            }

            override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
        }).dispatch()
    }

    private fun saveToFirestore(imageUrl: String?, name: String, phone: String, address: String) {
        val data = mutableMapOf<String, Any>(
            "name" to name,
            "phoneNumber" to phone,
            "address" to address
        )
        if (imageUrl != null) data["profileImage"] = imageUrl

        uid?.let {
            db.collection("users").document(it).update(data)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile Saved and Synced!", Toast.LENGTH_SHORT).show()
                    finish() // Update ke baad wapas home par bhej do
                }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Do you really want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                val intent = Intent(this, signupActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }
}