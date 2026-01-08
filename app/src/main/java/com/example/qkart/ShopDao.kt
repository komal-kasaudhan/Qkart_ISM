package com.example.qkart

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ShopDao {
    @Query("SELECT * FROM shop_table")
    suspend fun getAllShops(): List<Shop>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShops(shops: List<Shop>)

    @Query("DELETE FROM shop_table")
    suspend fun deleteAllShops()
    @Query("SELECT * FROM shop_table WHERE id = :shopId LIMIT 1")
    suspend fun getShopById(shopId: String): Shop?

}