package com.example.qkart

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class AddShopActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null
    private val db = FirebaseFirestore.getInstance()

    private var isEditMode = false
    private var existingShopId: String? = null
    private var oldImageUrl: String? = null
    private lateinit var switchOpen: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_shop)


        val imgPreview = findViewById<ImageView>(R.id.img_shop_preview)
        val btnSave = findViewById<Button>(R.id.btn_save_all)
        val imgHome = findViewById<ImageView>(R.id.img_home)
        val etName = findViewById<EditText>(R.id.et_shop_name)
        val etDesc = findViewById<EditText>(R.id.et_shop_desc)
        val etRating = findViewById<EditText>(R.id.et_shop_rating)
        val etPrepTime = findViewById<EditText>(R.id.et_prep_time)
        val etShopTime = findViewById<EditText>(R.id.et_shop_time)


        switchOpen = findViewById(R.id.switch_shop_open)

        // 2. STATUS BAR PADDING
        val rootView = findViewById<android.view.View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        // 3. SWITCH LISTENER (Initialize hone ke baad hi chalega)
        switchOpen.setOnCheckedChangeListener { _, isChecked ->
            if (isEditMode && existingShopId != null) {
                db.collection("shops").document(existingShopId!!)
                    .update("isAvailable", isChecked)
                    .addOnSuccessListener {
                        val statusText = if (isChecked) "Open" else "Closed"
                        Toast.makeText(this, "Shop is now $statusText", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Update failed!", Toast.LENGTH_SHORT).show()
                        switchOpen.isChecked = !isChecked
                    }
            }
        }

        // Init Cloudinary
        try { MediaManager.init(this) } catch (e: Exception) {}

        // 4. HANDLE EDIT MODE
        isEditMode = intent.getBooleanExtra("IS_EDIT", false)
        if (isEditMode) {
            existingShopId = intent.getStringExtra("SHOP_ID")
            oldImageUrl = intent.getStringExtra("IMAGE")


            val statusFromServer = intent.getBooleanExtra("IS_OPEN", true)
            switchOpen.isChecked = statusFromServer

            etName.setText(intent.getStringExtra("NAME"))
            etDesc.setText(intent.getStringExtra("DESC"))
            etRating.setText(intent.getDoubleExtra("RATING", 0.0).toString())
            etPrepTime.setText(intent.getStringExtra("PREP"))
            etShopTime.setText(intent.getStringExtra("TIME"))
            btnSave.text = "Update Shop Details"

            Glide.with(this).load(oldImageUrl).placeholder(R.drawable.add_a_photo).into(imgPreview)
        }

        // 5. CLICK LISTENERS
        imgHome.setOnClickListener {
            startActivity(Intent(this, AdminDashboardActivity::class.java))
            finish()
        }

        val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                imgPreview.setImageURI(uri)
            }
        }
        imgPreview.setOnClickListener { imagePicker.launch("image/*") }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            val isAvailable = switchOpen.isChecked
            val ratingInput = etRating.text.toString().toDoubleOrNull() ?: 0.0
            val prep = etPrepTime.text.toString().trim()
            val timing = etShopTime.text.toString().trim()

            if (name.isNotEmpty() && prep.isNotEmpty() && timing.isNotEmpty()) {
                if (selectedImageUri != null) {
                    uploadImageToCloudinary(name, desc, isAvailable, ratingInput, prep, timing)
                } else if (isEditMode && oldImageUrl != null) {
                    saveToFirestore(name, desc, oldImageUrl!!, isAvailable, ratingInput, prep, timing)
                } else {
                    Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImageToCloudinary(name: String, desc: String, isAvailable: Boolean, ratingInput: Double, prep: String, timing: String) {
        MediaManager.get().upload(selectedImageUri)
            .option("unsigned", true)
            .option("upload_preset", "zl6numjv")
            .option("cloud_name", "db2cwflo3")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val imageUrl = resultData?.get("secure_url").toString()
                    saveToFirestore(name, desc, imageUrl, isAvailable, ratingInput, prep, timing)
                }
                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Toast.makeText(this@AddShopActivity, "Cloudinary Error: ${error?.description}", Toast.LENGTH_SHORT).show()
                }
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            }).dispatch()
    }

    private fun saveToFirestore(name: String, desc: String, image: String, isAvailable: Boolean, rating: Double, prep: String, timing: String) {
        val shopId = if (isEditMode) existingShopId!! else db.collection("shops").document().id

        val shopData = hashMapOf(
            "id" to shopId,
            "name" to name,
            "description" to desc,
            "image" to image,
            "isAvailable" to isAvailable,
            "rating" to rating,
            "prepTime" to prep,
            "shopTime" to timing
        )

        db.collection("shops").document(shopId).set(shopData, SetOptions.merge()).addOnSuccessListener {
            val msg = if (isEditMode) "Shop updated successfully!" else "Shop added successfully!"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Firestore Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}