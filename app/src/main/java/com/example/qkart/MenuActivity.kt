package com.example.qkart

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.qkart.databinding.ActivityMenuBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuBinding
    private lateinit var menuAdapter: UserMenuAdapter
    private lateinit var specialAdapter: SpecialComboAdapter
    private val db = FirebaseFirestore.getInstance()
    private var shopId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shopId = intent.getStringExtra("SHOP_ID") ?: ""

        setupRecyclerView()
        setupUI()
        syncFirebaseToRoom()
        observeRoomData()
        setupSearch()
    }

    // 🌟 Fix: Jab CartActivity se back aao, toh strip update honi chahiye
    override fun onResume() {
        super.onResume()
        super.onPostResume()
        checkCartStatusOnResume()
    }

    private fun checkCartStatusOnResume() {
        lifecycleScope.launch(Dispatchers.IO) {
            val cartDao = AppDatabase.getDatabase(this@MenuActivity).cartDao()
            val totalAmount = cartDao.getTotalAmount() ?: 0.0
            val totalCount = cartDao.getTotalCount() ?: 0
            withContext(Dispatchers.Main) {
                updateCartStrip(totalAmount, totalCount)
            }
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        binding.tvMenuShopName.text = intent.getStringExtra("SHOP_NAME") ?: "Shop"
        binding.shopDesc.text = intent.getStringExtra("SHOP_TIME") ?: "9:00 AM - 10:00 PM"
        binding.tvPrepTime.text = intent.getStringExtra("PREP_TIME") ?: "20-25 mins"

        val rating = intent.getDoubleExtra("SHOP_RATING", 0.0)
        binding.tvRating.text = "$rating ★"

        Glide.with(this)
            .load(intent.getStringExtra("SHOP_IMAGE"))
            .placeholder(R.drawable.pizza)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(binding.imgShopBanner)

        // 🌟 ID check: cartStrip ya cartExtensionLayout jo bhi aapke XML mein hai
        binding.cartStrip.setOnClickListener {
            val intent = Intent(this@MenuActivity, CartActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        menuAdapter = UserMenuAdapter { item, qty ->
            handleCartUpdate(item, qty)
        }
        binding.rvMenuItems.layoutManager = LinearLayoutManager(this)
        binding.rvMenuItems.adapter = menuAdapter

        specialAdapter = SpecialComboAdapter { item, qty ->
            handleCartUpdate(item, qty)
        }
        binding.rvSpecials.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSpecials.adapter = specialAdapter
    }

    private fun handleCartUpdate(item: MenuItem, qty: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val cartDao = AppDatabase.getDatabase(this@MenuActivity).cartDao()

            val existingItems = cartDao.getAllCartItems()
            if (existingItems.isNotEmpty() && existingItems[0].shopId != shopId) {
                withContext(Dispatchers.Main) {
                    showClearCartDialog(item, qty)
                }
                return@launch
            }

            if (qty > 0) {
                // 🌟 Price Logic Update: No discount means single price
                val finalCalculatedPrice = item.calculateDiscountedPriceFromOffer()

                val cartItem = CartItem(
                    id = item.id,
                    name = item.name,
                    image = item.image,
                    originalPrice  = item.price,
                    discountedPrice = if (finalCalculatedPrice > 0) finalCalculatedPrice else item.price,
                    quantity = qty,
                    shopId = shopId
                )
                cartDao.update(cartItem)
            } else {
                cartDao.deleteById(item.id)
            }

            val totalAmount = cartDao.getTotalAmount() ?: 0.0
            val totalCount = cartDao.getTotalCount() ?: 0

            withContext(Dispatchers.Main) {
                updateCartStrip(totalAmount, totalCount)
            }
        }
    }

    private fun showClearCartDialog(newItem: MenuItem, newQty: Int) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Replace cart item?")
        builder.setMessage("Your cart contains items from another shop. Do you want to clear the cart and add items from this shop instead?")

        builder.setPositiveButton("Yes, Clear Cart") { _, _ ->
            lifecycleScope.launch(Dispatchers.IO) {
                val cartDao = AppDatabase.getDatabase(this@MenuActivity).cartDao()
                cartDao.deleteAll()
                withContext(Dispatchers.Main) {
                    handleCartUpdate(newItem, newQty)
                }
            }
        }
        builder.setNegativeButton("No", null)
        builder.show()
    }

    private fun syncFirebaseToRoom() {
        db.collection("shops").document(shopId).collection("menu")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                snapshots?.let {
                    val items = it.toObjects(MenuItem::class.java).onEach { item ->
                        item.shopId = shopId
                        item.isSpecialItem = false
                    }
                    lifecycleScope.launch(Dispatchers.IO) {
                        AppDatabase.getDatabase(this@MenuActivity).menuDao().insertAll(items)
                    }
                }
            }

        db.collection("shops").document(shopId).collection("special_combos")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                snapshots?.let {
                    val specials = it.toObjects(MenuItem::class.java).onEach { item ->
                        item.shopId = shopId
                        item.isSpecialItem = true
                    }
                    lifecycleScope.launch(Dispatchers.IO) {
                        AppDatabase.getDatabase(this@MenuActivity).menuDao().insertAll(specials)
                    }
                }
            }
    }

    private fun observeRoomData() {
        val dbRoom = AppDatabase.getDatabase(this)
        dbRoom.menuDao().getNormalMenu(shopId).observe(this) { items ->
            if (items != null) menuAdapter.submitList(items)
        }
        dbRoom.menuDao().getSpecialCombos(shopId).observe(this) { specials ->
            if (specials != null) specialAdapter.submitList(specials)
        }
    }

    private fun setupSearch() {
        binding.etMenuSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                menuAdapter.filter(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun updateCartStrip(totalPrice: Double, totalQuantity: Int) {
        if (totalQuantity > 0) {
            binding.cartStrip.visibility = View.VISIBLE
            // 🌟 Smooth Text Update
            binding.tvCartQuantity.text = "$totalQuantity Items | ₹${totalPrice.toInt()}"
        } else {
            binding.cartStrip.visibility = View.GONE
        }
    }
}