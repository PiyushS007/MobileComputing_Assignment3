package com.example.myapplication3_testing

import androidx.room.Database
import androidx.room.RoomDatabase

import androidx.room.Entity
import androidx.room.PrimaryKey

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Database(entities = [AccelerometerData::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accelerometerDataDao(): AccelerometerDataDao
}





@Entity(tableName = "accelerometer_data")
data class AccelerometerData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val x: Float,
    val y: Float,
    val z: Float
)



@Dao
interface AccelerometerDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: AccelerometerData)

    @Query("SELECT * FROM accelerometer_data")
    suspend fun getAll(): List<AccelerometerData>
}
