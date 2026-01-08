package com.example.qkart

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class AdminMenuActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminMenuAdapter
    private val db = FirebaseFirestore.getInstance()
    private var menuList = mutableListOf<MenuItem>()
    private var shopId: String = ""
    private var currentCollection = "menu"
    private var firestoreListener: ListenerRegistration? = null

    // 🌟 Room Database Instance
    private lateinit var roomDb: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_menu)

        shopId = intent.getStringExtra("SHOP_ID") ?: ""

        // 🌟 Room Database Initialize
        roomDb = AppDatabase.getDatabase(this)

        val imgHome = findViewById<ImageView>(R.id.btn_back_dashboard)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fab_add_item)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayoutAdmin)
        recyclerView = findViewById(R.id.rv_menu_items)


        adapter = AdminMenuAdapter(
            menuList,
            shopId,
            currentCollection,
            onEditClick = { item -> openEditScreen(item) },
            onDeleteClick = { itemId, category -> deleteItemFromFirebase(itemId, category) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentCollection = if (tab?.position == 0) "menu" else "special_combos"
                fetchMenuFromFirestore()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        fabAdd.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.admin_add_choice_menu, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                val intent = Intent(this, SheetAddItemActivity::class.java)
                intent.putExtra("SHOP_ID", shopId)
                when (menuItem.itemId) {
                    R.id.add_regular -> intent.putExtra("IS_SPECIAL", false)
                    R.id.add_special -> intent.putExtra("IS_SPECIAL", true)
                }
                startActivity(intent)
                true
            }
            popup.show()
        }

        imgHome.setOnClickListener { finish() }

        fetchMenuFromFirestore()
    }

    private fun fetchMenuFromFirestore() {
        firestoreListener?.remove()

        firestoreListener = db.collection("shops").document(shopId)
            .collection(currentCollection)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val items = snapshots.toObjects(MenuItem::class.java)

                    // 🌟 1. Firebase se aane wale data mein flag set karein
                    items.forEach {
                        it.shopId = shopId
                        it.isSpecialItem = (currentCollection == "special_combos")
                    }

                    // 🌟 2. Room Database mein save karein (Background Thread par)
                    lifecycleScope.launch {
                        roomDb.menuDao().insertAll(items)
                    }

                    // 3. UI update karein
                    adapter.updateList(items, currentCollection)
                }
            }
    }

    private fun deleteItemFromFirebase(itemId: String, category: String) {
        db.collection("shops").document(shopId).collection(category).document(itemId)
            .delete()
            .addOnSuccessListener {
                // 🌟 Room se bhi delete karein taaki data sync rahe
                lifecycleScope.launch {
                    val itemToDelete = MenuItem().apply { id = itemId }
                    roomDb.menuDao().deleteItem(itemToDelete)
                }
                Toast.makeText(this, "Deleted from $category", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openEditScreen(item: MenuItem) {
        val intent = Intent(this, SheetAddItemActivity::class.java)
        intent.putExtra("SHOP_ID", shopId)
        intent.putExtra("ITEM_ID", item.id)
        intent.putExtra("ITEM_NAME", item.name)
        intent.putExtra("ITEM_PRICE", item.price.toString())
        intent.putExtra("ITEM_DESC", item.description)
        intent.putExtra("ITEM_IMAGE", item.image)
        intent.putExtra("ITEM_OFFER", item.offer)
        intent.putExtra("IS_EDIT", true)
        intent.putExtra("IS_SPECIAL", currentCollection == "special_combos")
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        firestoreListener?.remove()
    }
}
