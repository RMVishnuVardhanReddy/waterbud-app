package com.waterbud.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A single logged drink. Volume is stored in milliliters (ml) always. */
@Entity(tableName = "water_log")
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Epoch millis when the drink was logged. */
    val timestamp: Long,
    /** Local date key "yyyy-MM-dd" for fast per-day grouping. */
    val dayKey: String,
    /** Actual volume consumed, in ml. */
    val amountMl: Int,
    /** Hydrating value in ml after applying the drink coefficient (e.g. coffee < water). */
    val hydrationMl: Int,
    /** Drink type id, e.g. "water", "coffee", "tea". */
    val drinkType: String = "water"
)

/** A user-defined quick-add cup (e.g. "Bottle 500 ml"). */
@Entity(tableName = "cup")
data class Cup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val amountMl: Int,
    val drinkType: String = "water",
    val sortOrder: Int = 0
)
