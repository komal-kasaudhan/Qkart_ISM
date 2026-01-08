package com.example.qkart

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase



@Database(entities = [MenuItem::class, Shop::class,CartItem::class,PickupProfile::class], version = 430, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {


    abstract fun menuDao(): MenuDao
    abstract fun shopDao(): ShopDao
    abstract fun cartDao(): CartDao
    abstract fun pickupDao(): PickupDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "qkart_db"
                )
                    .fallbackToDestructiveMigration() // Version change hone par purana data clear karke naya structure banayega
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}