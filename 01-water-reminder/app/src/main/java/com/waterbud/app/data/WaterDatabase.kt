package com.waterbud.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WaterLog::class, Cup::class], version = 1, exportSchema = false)
abstract class WaterDatabase : RoomDatabase() {
    abstract fun waterDao(): WaterDao

    companion object {
        @Volatile private var INSTANCE: WaterDatabase? = null

        fun get(context: Context): WaterDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WaterDatabase::class.java,
                    "waterbud.db"
                )
                    // Never destructively wipe user data silently. Add real migrations
                    // here when version increments. (Fixes the "lost all my data" complaint.)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
