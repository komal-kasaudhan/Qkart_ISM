package com.example.qkart

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.qkart.databinding.ActivityAdminProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)


        initCloudinary()


        binding.tvAdminEmail.text = auth.currentUser?.email

        // 3. Name, Phone aur Photo Firestore load
        loadAdminExtraDetails()

        // 4. Back Button
        binding.imgProfileBack.setOnClickListener { finish() }

        // 5. Photo Pick Launcher
        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imageUri = uri
                binding.ivAdminProfile.setImageURI(uri)
                uploadImageToCloudinary(uri)
            }
        }

        binding.ivAdminProfile.setOnClickListener {
            pickImage.launch("image/*")
        }

        // 6. Phone Update Logic
        binding.btnUpdatePhone.setOnClickListener {
            val phone = binding.etAdminPhone.text.toString()
            if (phone.length == 10) {
                updatePhoneNumber(phone)
            } else {
                Toast.makeText(this, "Enter valid 10 digit number", Toast.LENGTH_SHORT).show()
            }
        }

        // 7. LOGOUT Logic
        binding.btnLogout.setOnClickListener {
            val sharedPref = getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE)
            sharedPref.edit().clear().apply()
            auth.signOut()
            val intent = Intent(this, signupActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }


    private fun initCloudinary() {
        try {
            val config = HashMap<String, String>()
            config["cloud_name"] = "db2cwflo3"
            config["api_key"] = "854955711338177"
            config["api_secret"] = "fS8L86doayXdxF6CSYYQohF7H9I"
            MediaManager.init(this, config)
        } catch (e: Exception) {

        }
    }

    private fun loadAdminExtraDetails() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("admins").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val nameFromDB = doc.getString("name")
                    val phoneFromDB = doc.getString("phoneNumber")
                    val imageFromDB = doc.getString("profileImage")

                    binding.tvAdminName.text = nameFromDB ?: "Admin User"
                    binding.etAdminPhone.setText(phoneFromDB ?: "")

                    if (!imageFromDB.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(imageFromDB)
                            .placeholder(R.drawable.account_circle)
                            .into(binding.ivAdminProfile)
                    }
                }
            }
    }


    private fun uploadImageToCloudinary(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return

        Toast.makeText(this, "Uploading to Cloudinary...", Toast.LENGTH_SHORT).show()


        val options = HashMap<String, Any>()
        options["upload_preset"] = "zl6numjv"
        options["cloud_name"] = "db2cwflo3"
        options["api_key"] = "854955711338177"
        options["api_secret"] = "fS8L86doayXdxF6CSYYQohF7H9I"

        MediaManager.get().upload(uri)
            .options(options)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) { }
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) { }

                override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                    val imageUrl = resultData?.get("secure_url").toString()


                    db.collection("admins").document(uid)
                        .update("profileImage", imageUrl)
                        .addOnSuccessListener {
                            Toast.makeText(this@AdminProfileActivity, "Profile Photo Updated!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@AdminProfileActivity, "Firestore Update Failed", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {

                    Toast.makeText(this@AdminProfileActivity, "Cloudinary Error: ${error?.description}", Toast.LENGTH_LONG).show()
                    android.util.Log.e("CLOUDINARY_ERROR", error?.description ?: "Unknown Error")
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) { }
            }).dispatch()
    }

    private fun updatePhoneNumber(phone: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("admins").document(uid).update("phoneNumber", phone)
            .addOnSuccessListener {
                Toast.makeText(this, "Phone Number Updated!", Toast.LENGTH_SHORT).show()
            }
    }
}