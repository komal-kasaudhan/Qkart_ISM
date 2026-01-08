package com.example.qkart

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
import com.example.qkart.databinding.ActivitySheetAddItemBinding
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SheetAddItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySheetAddItemBinding
    private var imageUri: Uri? = null
    private var existingImageUrl: String? = null
    private var itemId: String? = null
    private var shopId: String? = null
    private var isSpecial: Boolean = false // 🌟 Naya flag category track karne ke liye

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySheetAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Shop ID, Item ID aur IS_SPECIAL flag fetch karein
        shopId = intent.getStringExtra("SHOP_ID")
        itemId = intent.getStringExtra("ITEM_ID")
        isSpecial = intent.getBooleanExtra("IS_SPECIAL", false) // 🌟 AdminMenu se aa raha hai

        if (shopId == null) {
            Toast.makeText(this, "Error: Shop ID missing!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // UI Header Update (Optional: Admin ko pata chale kya add kar raha hai)
        if (isSpecial) {
            // Agar aapke layout mein header textview hai toh use change kar sakte hain
            // binding.tvHeaderTitle.text = "Add Special Combo"
        }

        // 2. Edit Mode Logic
        if (itemId != null) {
            setupEditMode()
        }

        // Navigation: Back button
        binding.btnBackToMenu.setOnClickListener {
            finish() // Simple finish is better to go back to previous state
        }

        // Image Picker Logic
        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imageUri = uri
                binding.imgItemUploadPreview.setImageURI(uri)
            }
        }
        binding.cardItemImage.setOnClickListener { pickImage.launch("image/*") }

        // Save/Update Button
        binding.btnSaveAndUpload.setOnClickListener {
            validateData()
        }
    }

    private fun setupEditMode() {
        val oldName = intent.getStringExtra("ITEM_NAME")
        val oldPrice = intent.getStringExtra("ITEM_PRICE")
        val oldDesc = intent.getStringExtra("ITEM_DESC")
        val oldOffer = intent.getStringExtra("ITEM_OFFER")
        existingImageUrl = intent.getStringExtra("ITEM_IMAGE")

        binding.etNewItemName.setText(oldName)
        binding.etNewItemPrice.setText(oldPrice)
        binding.etNewItemDesc.setText(oldDesc)
        binding.tvItemOfferAdmin.setText(oldOffer)
        binding.btnSaveAndUpload.text = "Update Item"

        if (!existingImageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(existingImageUrl)
                .placeholder(R.drawable.signimg)
                .into(binding.imgItemUploadPreview)
        }
    }

    private fun validateData() {
        val name = binding.etNewItemName.text.toString().trim()
        val price = binding.etNewItemPrice.text.toString().trim()
        val desc = binding.etNewItemDesc.text.toString().trim()
        val offer = binding.tvItemOfferAdmin.text.toString().trim()

        if (name.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "Please fill required fields!", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSaveAndUpload.isEnabled = false

        if (imageUri != null) {
            uploadToCloudinary(imageUri!!) { newUrl ->
                saveToFirestore(name, price, desc, offer, newUrl)
            }
        } else if (!existingImageUrl.isNullOrEmpty()) {
            saveToFirestore(name, price, desc, offer, existingImageUrl!!)
        } else {
            binding.btnSaveAndUpload.isEnabled = true
            Toast.makeText(this, "Please select an image!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadToCloudinary(uri: Uri, callback: (String) -> Unit) {
        MediaManager.get().upload(uri)
            .option("unsigned", true)
            .option("upload_preset", "zl6numjv")
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                    callback(resultData?.get("secure_url").toString())
                }
                override fun onError(requestId: String?, error: ErrorInfo?) {
                    binding.btnSaveAndUpload.isEnabled = true
                    Toast.makeText(this@SheetAddItemActivity, "Upload Failed", Toast.LENGTH_SHORT).show()
                }
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, b: Long, t: Long) {}
                override fun onReschedule(requestId: String?, e: ErrorInfo?) {}
            }).dispatch()
    }

    private fun saveToFirestore(name: String, price: String, desc: String, offer: String, url: String) {
        val db = FirebaseFirestore.getInstance()

        // 🌟 1. Collection Path decide karein
        val targetCollection = if (isSpecial) "special_combos" else "menu"

        // 🌟 2. Path dynamic banayein
        val collectionRef = db.collection("shops").document(shopId!!).collection(targetCollection)

        val finalId = itemId ?: collectionRef.document().id
        val roomDb = AppDatabase.getDatabase(this)
        val item = MenuItem(
            id = finalId,
            name = name,
            description = desc,
            image = url,
            price = price.toDoubleOrNull() ?: 0.0,
            isAvailable = true,
            offer = offer,
            rating = 0.0
        )

        collectionRef.document(finalId)
            .set(item)
            .addOnSuccessListener {
                lifecycleScope.launch {
                    roomDb.menuDao().insertItem(item)
                }
                Toast.makeText(this, if (itemId == null) "Added to $targetCollection!" else "Updated!", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                binding.btnSaveAndUpload.isEnabled = true
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}