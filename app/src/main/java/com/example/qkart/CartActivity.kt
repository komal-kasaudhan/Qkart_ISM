package com.example.qkart

import PickupAdapter
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.qkart.databinding.ActivityCartBinding
import com.example.qkart.databinding.LayoutAddProfileBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.*

class CartActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var binding: ActivityCartBinding
    private lateinit var cartAdapter: CartAdapter
    private lateinit var pickupAdapter: PickupAdapter

    private var cartList = mutableListOf<CartItem>()
    private var selectedProfileId: Int = -1
    private var deliveryTime: String = "Instant"

    // 🌟 Logic Variables
    private var itemTotal = 0.0
    private var storeDiscount = 0.0
    private var couponDiscount = 0.0
    private var xpDiscount = 0.0
    private var userTotalXP = 0 // 🌟 Real XP fetch hogi Firestore se
    private var earnedXP = 0
    private var grandTotalAmount: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Checkout.preload(applicationContext)

        setupRecyclerView()
        setupPickupRecyclerView()
        loadCartData()
        observePickupProfiles()
        setupListeners()
        setupDeliveryScheduling()
        fetchGlobalXP() // 🌟 XP Fetch trigger
    }

    // 💳 Razorpay Functions
    private fun startPayment() {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_Rz3tjDuLuIB8sC")

        try {
            val options = JSONObject()
            options.put("name", "QKart")
            options.put("description", "Food Order Payment")
            options.put("theme.color", "#3399cc")
            options.put("currency", "INR")

            val amountInPaise = (grandTotalAmount * 100).toInt()
            options.put("amount", amountInPaise.toString())

            val profile = pickupAdapter.getSelectedProfile()
            options.put("prefill.contact", profile?.phone ?: "")
            options.put("prefill.email", "customer@example.com")

            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Payment Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        saveOrderToFirebase()
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Payment Failed: $response", Toast.LENGTH_LONG).show()
    }

    // 🌟 1. Global XP Fetching Logic
    private fun fetchGlobalXP() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    userTotalXP = doc.getLong("totalXP")?.toInt() ?: 0
                }
            }
    }

    // 🛒 Core Calculation Logic (Fixed Multiplier & Logic)
    private fun calculateBill() {
        itemTotal = 0.0
        storeDiscount = 0.0

        cartList.forEach { item ->
            // 🌟 Price check to avoid 0.0 (multiplying with quantity)
            itemTotal += (item.originalPrice * item.quantity)
            storeDiscount += ((item.originalPrice - item.discountedPrice) * item.quantity)
        }

        // 🌟 XP Earning: ₹100 pe 10 XP (Har complete 100 pe)
        val netForXP = (itemTotal - storeDiscount)
        earnedXP = (netForXP / 100).toInt() * 10

        // 🌟 Final Calculation with One-at-a-time restriction
        val grandTotal = (itemTotal - storeDiscount) - couponDiscount - xpDiscount
        grandTotalAmount = if (grandTotal > 0) grandTotal else 0.0

        // UI Updates (Using your XML IDs)
        binding.tvItemTotal.text = "₹${itemTotal.toInt()}"
        binding.tvProductDiscount.text = "-₹${storeDiscount.toInt()}"

        // 🌟 Coupon Row Visibility
        if (couponDiscount > 0) {
            binding.rlCouponDiscount.visibility = View.VISIBLE
            binding.tvCouponDiscount.text = "-₹${couponDiscount.toInt()}"
        } else {
            binding.rlCouponDiscount.visibility = View.GONE
        }

        // 🌟 XP Row Visibility
        if (xpDiscount > 0) {
            binding.rlXpDiscount.visibility = View.VISIBLE
            binding.tvXpDiscount.text = "-₹${xpDiscount.toInt()}"
        } else {
            binding.rlXpDiscount.visibility = View.GONE
        }

        binding.tvGrandTotal.text = "₹${grandTotalAmount.toInt()}"
        binding.tvBottomPrice.text = "₹${grandTotalAmount.toInt()}"

        // Savings Banner Logic
        val totalSavings = storeDiscount + couponDiscount + xpDiscount
        if (totalSavings > 0) {
            binding.tvSavingsAmount.visibility = View.VISIBLE
            binding.tvSavingsAmount.text = "You are saving ₹${totalSavings.toInt()} on this order! 😍"
        } else {
            binding.tvSavingsAmount.visibility = View.GONE
        }

        binding.tvCartItemCount.text = "${cartList.size} Items in Cart"
    }

    private fun setupListeners() {
        binding.btnBackCart.setOnClickListener { finish() }

        // 🌟 Payment Mode Switch Logic (Enable/Disable Offers)
        binding.rgPayment.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbOnline) {
                binding.offerCard.alpha = 1.0f
                binding.btnApplyCoupon.isEnabled = true
                binding.switchUseXp.isEnabled = true
            } else {
                // COD/Pay at Shop Logic: Reset all offers
                binding.offerCard.alpha = 0.5f
                binding.btnApplyCoupon.isEnabled = false
                binding.switchUseXp.isEnabled = false
                binding.switchUseXp.isChecked = false
                couponDiscount = 0.0
                xpDiscount = 0.0
                binding.etCouponCode.text.clear()
                calculateBill()
            }
        }

        // 🌟 Coupon Apply Logic with Mutual Exclusion
        binding.btnApplyCoupon.setOnClickListener {
            if (binding.switchUseXp.isChecked) {
                Toast.makeText(this, "Remove XP points to use Coupon!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showCouponSelectionDialog()
        }

        // 🌟 XP Redeem Logic (10 XP = ₹1) with Mutual Exclusion
        binding.switchUseXp.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (couponDiscount > 0) {
                    binding.switchUseXp.isChecked = false
                    Toast.makeText(this, "Coupon already applied!", Toast.LENGTH_SHORT).show()
                    return@setOnCheckedChangeListener
                }
                // Redemption: 10 XP = ₹1 discount
                xpDiscount = (userTotalXP / 10).toDouble()
                if (xpDiscount > 0) {
                    Toast.makeText(this, "₹${xpDiscount.toInt()} Discount Applied via XP!", Toast.LENGTH_SHORT).show()
                }
            } else {
                xpDiscount = 0.0
            }
            calculateBill()
        }

        binding.btnPlaceOrder.setOnClickListener {
            if (selectedProfileId == -1) Toast.makeText(this, "Select Profile!", Toast.LENGTH_SHORT).show()
            else {
                if (binding.rbOnline.isChecked) startPayment() else saveOrderToFirebase()
            }
        }

        binding.btnChangeProfile.setOnClickListener { showAddProfileBottomSheet(null) }
        binding.btnAddMoreItems.setOnClickListener {
            val shopId = cartList.firstOrNull()?.shopId
            if (shopId != null) {
                val intent = Intent(this, MenuActivity::class.java).apply {
                    putExtra("SHOP_ID", shopId)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                finish()
            }
        }
    }

    // 🌟 2. Shop-Specific Coupon Dialog
    private fun showCouponSelectionDialog() {
        val currentShopId = cartList.firstOrNull()?.shopId ?: ""
        FirebaseFirestore.getInstance().collection("coupons")
            .whereEqualTo("shopId", currentShopId)
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots.isEmpty) {
                    Toast.makeText(this, "No coupons for this shop!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // 🌟 List taiyar kar rahe hain: "Code - ₹Amount OFF"
                val codes = snapshots.documents.map { it.getString("code") ?: "" }
                val amounts = snapshots.documents.map { it.get("discountPrice").toString().toDoubleOrNull() ?: 0.0 }
                val minOrders = snapshots.documents.map { it.get("minOrderValue").toString().toDoubleOrNull() ?: 0.0 }

                // User ko dikhane ke liye formatted string array
                val displayOptions = codes.indices.map { i ->
                    "${codes[i]} - (₹${amounts[i].toInt()} OFF on ₹${minOrders[i].toInt()}+)"
                }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Select Your Coupon")
                    .setItems(displayOptions) { _, which ->
                        val subTotal = itemTotal - storeDiscount

                        // Min order check
                        if (subTotal >= minOrders[which]) {
                            couponDiscount = amounts[which]
                            binding.etCouponCode.setText(codes[which])
                            calculateBill()
                            Toast.makeText(this, "${codes[which]} Applied! ₹${amounts[which].toInt()} Saved", Toast.LENGTH_SHORT).show()
                        } else {
                            val needed = minOrders[which] - subTotal
                            Toast.makeText(this, "Add ₹${needed.toInt()} more to use this!", Toast.LENGTH_LONG).show()
                        }
                    }.show()
            }
    }

    private fun saveOrderToFirebase() {
        val selectedProfile = pickupAdapter.getSelectedProfile()
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return

        val progressDialog = AlertDialog.Builder(this)
            .setMessage("Placing order...")
            .setCancelable(false)
            .show()

        val db = FirebaseFirestore.getInstance()
        val orderId = "ORD${System.currentTimeMillis()}"

        val orderData = hashMapOf(
            "orderId" to orderId,
            "userId" to currentUser.uid,
            "shopId" to (cartList.firstOrNull()?.shopId ?: ""),
            "shopName" to binding.tvCartShopName.text.toString(),
            "userName" to selectedProfile!!.name,
            "userPhone" to selectedProfile.phone,
            "items" to cartList,
            "totalAmount" to grandTotalAmount,
            "couponDiscount" to couponDiscount,
            "xpDiscount" to xpDiscount,
            "earnedXP" to earnedXP, // 🌟 Order history ke liye
            "deliveryTiming" to deliveryTime,
            "paymentMode" to (if (binding.rbOnline.isChecked) "Online" else "COD"),
            "status" to "Placed",
            "timestamp" to System.currentTimeMillis()
        )


        db.collection("orders").document(orderId)
            .set(orderData)
            .addOnSuccessListener {
                progressDialog.dismiss()
                updatePointsAfterOrder(currentUser.uid) // 🌟 XP Update trigger
                lifecycleScope.launch(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@CartActivity).cartDao().deleteAll()
                    withContext(Dispatchers.Main) { showOrderSuccessDialog() }
                }
            }
    }
    private fun sendNotificationToAdmin(orderId: String) {
        // Note: Asli production mein ye kaam "Firebase Functions" se hota hai,
        // par "Easy Way" ke liye aap ek choti API call ya Firestore Trigger use kar sakte hain.

        // Easy Way for Testing:
        // Abhi ke liye aap Firestore mein ek "notifications" collection bana sakte hain,
        // jise Admin App addSnapshotListener se listen karega aur khud notification bajayega.
    }
    // 🌟 3. XP Sync Logic (Global Profile)
    private fun updatePointsAfterOrder(uid: String) {
        val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)

        // Agar XP use hui toh purani khatam (0), aur nayi add
        // Agar XP use nahi hui toh sirf nayi add
        if (binding.switchUseXp.isChecked) {
            userRef.update("totalXP", earnedXP.toLong())
        } else {
            userRef.update("totalXP", FieldValue.increment(earnedXP.toLong()))
        }
    }

    // --- UTILITY FUNCTIONS (NO CHANGE) ---
    private fun showOrderSuccessDialog() {
        // 🌟 Notification Trigger add
        val currentOrderId = "ORD${System.currentTimeMillis()}"
        val userName = pickupAdapter.getSelectedProfile()?.name ?: "Customer"

        triggerNotificationForAdmin(currentOrderId, userName)

        val dialog = android.app.Dialog(this)
        val view = layoutInflater.inflate(R.layout.layout_order_success, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)
        view.findViewById<android.widget.Button>(R.id.btnDone).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
        }
        dialog.show()
    }


    private fun triggerNotificationForAdmin(orderId: String, customerName: String) {
        val adminNotif = hashMapOf(
            "title" to "New Order Received! 🍔",
            "message" to "$customerName has placed order of id $orderId.",
            "shopId" to (cartList.firstOrNull()?.shopId ?: ""), // Cart se shopId uthayega
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("admin_notifications")
            .add(adminNotif)
    }

    private fun loadCartData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@CartActivity)
            val items = db.cartDao().getAllCartItems()
            withContext(Dispatchers.Main) {
                if (items.isNotEmpty()) {
                    cartList.clear()
                    cartList.addAll(items)
                    cartAdapter.submitList(cartList.toList())
                    val shop = db.shopDao().getShopById(items[0].shopId)
                    binding.tvCartShopName.text = shop?.name
                    Glide.with(this@CartActivity).load(shop?.image).into(binding.ivCartShopImage)
                    calculateBill()
                } else finish()
            }
        }
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(
            onQuantityChanged = { updateItemInRoom(it) },
            onDeleteRequested = { item, pos -> showDeleteDialog(item, pos) }
        )
        binding.rvCartItems.layoutManager = LinearLayoutManager(this)
        binding.rvCartItems.adapter = cartAdapter
    }

    private fun setupPickupRecyclerView() {
        pickupAdapter = PickupAdapter(
            onEdit = { showAddProfileBottomSheet(it) },
            onDelete = { deleteProfile(it) },
            onSelect = { selectProfile(it) }
        )
        binding.rvPickupProfiles.apply {
            layoutManager = LinearLayoutManager(this@CartActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = pickupAdapter
        }
    }

    private fun observePickupProfiles() {
        AppDatabase.getDatabase(this).pickupDao().getAllProfiles().observe(this) { profiles ->
            pickupAdapter.submitList(profiles)
            selectedProfileId = profiles.find { it.isSelected }?.id ?: -1
        }
    }

    private fun updateItemInRoom(item: CartItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(this@CartActivity).cartDao().update(item)
            loadCartData()
        }
    }

    private fun showDeleteDialog(item: CartItem, pos: Int) {
        AlertDialog.Builder(this).setMessage("Remove ${item.name}?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    AppDatabase.getDatabase(this@CartActivity).cartDao().delete(item)
                    loadCartData()
                }
            }.show()
    }

    private fun selectProfile(profile: PickupProfile) {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getDatabase(this@CartActivity).pickupDao()
            dao.unselectAllProfiles()
            profile.isSelected = true
            dao.updateProfile(profile)
        }
    }

    private fun setupDeliveryScheduling() {
        binding.rgDeliveryTiming.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbSchedule) {
                val cal = Calendar.getInstance()
                TimePickerDialog(this, { _, h, m ->
                    val ampm = if (h < 12) "AM" else "PM"
                    val displayH = if (h > 12) h - 12 else if (h == 0) 12 else h
                    deliveryTime = String.format("%02d:%02d %s", displayH, m, ampm)
                    binding.layoutSelectedTime.visibility = View.VISIBLE
                    binding.tvSelectedTime.text = "Scheduled for: $deliveryTime"
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
            } else {
                deliveryTime = "Instant"
                binding.layoutSelectedTime.visibility = View.GONE
            }
        }
    }

    private fun showAddProfileBottomSheet(profile: PickupProfile?) {
        val dialog = BottomSheetDialog(this)
        val bnd = LayoutAddProfileBinding.inflate(layoutInflater)
        dialog.setContentView(bnd.root)
        profile?.let {
            bnd.etName.setText(it.name); bnd.etPhone.setText(it.phone)
            bnd.etHostel.setText(it.hostel); bnd.etRoom.setText(it.roomNo)
        }
        bnd.btnSaveProfile.setOnClickListener {
            val n = bnd.etName.text.toString(); val p = bnd.etPhone.text.toString()
            if (n.isEmpty() || p.isEmpty()) return@setOnClickListener
            lifecycleScope.launch(Dispatchers.IO) {
                val dao = AppDatabase.getDatabase(this@CartActivity).pickupDao()
                if (profile == null) dao.insertProfile(PickupProfile(0, n, p, "", bnd.etHostel.text.toString(), bnd.etRoom.text.toString(), false))
                else dao.updateProfile(profile.copy(name = n, phone = p, hostel = bnd.etHostel.text.toString(), roomNo = bnd.etRoom.text.toString()))
                withContext(Dispatchers.Main) { dialog.dismiss() }
            }
        }
        dialog.show()
    }

    private fun deleteProfile(profile: PickupProfile) {
        lifecycleScope.launch(Dispatchers.IO) { AppDatabase.getDatabase(this@CartActivity).pickupDao().deleteProfile(profile) }
    }
}