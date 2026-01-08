package com.example.qkart

import android.content.Context // 🌟 Added
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.qkart.databinding.ActivityManageCouponsBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ManageCouponsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageCouponsBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: CouponAdapter
    private val couponList = mutableListOf<Coupon>()
    private var adminShopId: String? = null // 🌟 Added

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageCouponsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        adminShopId = getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE)
            .getString("SAVED_SHOP_ID", "")

        setupRecyclerView()

        binding.btnBackCoupons.setOnClickListener { finish() }

        binding.fabAddCoupon.setOnClickListener {
            showAddCouponDialog()
        }


        if (!adminShopId.isNullOrEmpty()) {
            loadCoupons()
        } else {
            Toast.makeText(this, "Shop ID not found!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = CouponAdapter(couponList) { coupon ->
            showDeleteConfirmation(coupon)
        }
        binding.rvCouponsList.layoutManager = LinearLayoutManager(this)
        binding.rvCouponsList.adapter = adapter
    }

    private fun showAddCouponDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_coupon, null)
        val etCode = view.findViewById<EditText>(R.id.etCouponCode)
        val etDiscount = view.findViewById<EditText>(R.id.etDiscountPrice)
        val etMinOrder = view.findViewById<EditText>(R.id.etMinOrder)
        val etLimit = view.findViewById<EditText>(R.id.etMaxLimit)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add New Coupon")
            .setView(view)
            .setPositiveButton("Create", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val code = etCode.text.toString().uppercase().trim()
            val discount = etDiscount.text.toString().toIntOrNull() ?: 0
            val minOrder = etMinOrder.text.toString().toIntOrNull() ?: 0
            val limit = etLimit.text.toString().toIntOrNull() ?: 0

            if (code.isEmpty() || discount <= 0 || minOrder < 0 || limit <= 0) {
                Toast.makeText(this, "Please fill all details correctly!", Toast.LENGTH_SHORT).show()
            } else {
                saveCouponToFirebase(code, discount, minOrder, limit)
                dialog.dismiss()
            }
        }
    }

    private fun saveCouponToFirebase(code: String, discount: Int, minOrder: Int, limit: Int) {
        val id = db.collection("coupons").document().id
        val generatedDesc = "Get Flat ₹$discount OFF on orders above ₹$minOrder"


        val coupon = Coupon(
            id = id,
            code = code,
            discountPrice = discount,
            shopId = adminShopId ?: "", // 🌟 Admin ki apni shop ID yahan save hogi
            minOrderValue = minOrder,
            maxUsageLimit = limit,
            description = generatedDesc,
            timestamp = System.currentTimeMillis()
        )

        db.collection("coupons").document(id).set(coupon)
            .addOnSuccessListener {
                Toast.makeText(this, "Coupon '$code' Live ", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadCoupons() {
        // 🌟 3. Filter Query: Admin ko sirf apni shop ke coupons dikhenge
        db.collection("coupons")
            .whereEqualTo("shopId", adminShopId) // 🌟 Filter added
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                couponList.clear()
                value?.documents?.forEach { doc ->
                    val coupon = doc.toObject(Coupon::class.java)
                    if (coupon != null) couponList.add(coupon)
                }
                adapter.notifyDataSetChanged()

                binding.tvNoCoupons.visibility = if (couponList.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
    }

    private fun showDeleteConfirmation(coupon: Coupon) {
        AlertDialog.Builder(this)
            .setTitle("Delete Coupon?")
            .setMessage("are you sure you want to delete ${coupon.code} ?")
            .setPositiveButton("Yes, Delete") { _, _ ->
                db.collection("coupons").document(coupon.id).delete()
                    .addOnSuccessListener { Toast.makeText(this, "Deleted!", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("No", null)
            .show()
    }
}