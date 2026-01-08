package com.example.qkart

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CartDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(item: CartItem)


    @Query("SELECT * FROM cart_table")
    suspend fun getAllCartItems(): List<CartItem>
    @Query("DELETE FROM cart_table")
    suspend fun deleteAll()


    @Query("DELETE FROM cart_table WHERE id = :itemId")
    suspend fun deleteById(itemId: String)


    @Delete
    suspend fun delete(item: CartItem)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cartItem: CartItem)


    @Query("SELECT SUM(discountedPrice * quantity) FROM cart_table")
    suspend fun getTotalAmount(): Double?


    @Query("SELECT SUM(quantity) FROM cart_table")
    suspend fun getTotalCount(): Int?


    @Query("DELETE FROM cart_table")
    suspend fun clearCart()
}