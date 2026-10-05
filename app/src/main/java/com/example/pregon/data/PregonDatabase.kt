package com.example.pregon.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [QueryEntity::class], version = 1, exportSchema = false)
abstract class PregonDatabase : RoomDatabase() {
    abstract fun queryDao(): QueryDao

    companion object {
        @Volatile
        private var INSTANCE: PregonDatabase? = null

        fun getDatabase(context: Context): PregonDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PregonDatabase::class.java,
                    "pregon_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
