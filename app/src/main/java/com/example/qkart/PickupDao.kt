


package com.example.qkart


import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PickupDao {

    @Query("SELECT * FROM pickup_profiles")
    fun getAllProfiles(): LiveData<List<PickupProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: PickupProfile)

    @Update
    suspend fun updateProfile(profile: PickupProfile)

    @Delete
    suspend fun deleteProfile(profile: PickupProfile)

    // Jab ek profile select ho, toh baaki sabko unselect karne ke liye
    @Query("UPDATE pickup_profiles SET isSelected = 0")
    suspend fun unselectAllProfiles()
}