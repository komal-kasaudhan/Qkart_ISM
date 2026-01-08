package com.example.qkart

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MenuDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: MenuItem)

    // 2. Bulk items
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MenuItem>)

    // 3. Normal Menu items fetch karne ke liye (isSpecialItem = 0)
    @Query("SELECT * FROM menu_table WHERE shopId = :sId AND isSpecialItem = 0")
    fun getNormalMenu(sId: String): LiveData<List<MenuItem>>

    // 4. Special Combos items fetch karne ke liye (isSpecialItem = 1)
    @Query("SELECT * FROM menu_table WHERE shopId = :sId AND isSpecialItem = 1")
    fun getSpecialCombos(sId: String): LiveData<List<MenuItem>>

    // 5. Single item delete karne ke liye
    @Delete
    suspend fun deleteItem(item: MenuItem)

    // 6. Kisi specific shop ka poora data saaf karne ke liye
    @Query("DELETE FROM menu_table WHERE shopId = :sId")
    suspend fun clearShopMenu(sId: String)
}