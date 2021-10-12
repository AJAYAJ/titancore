package com.titan.logger

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.*

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

@Dao
interface AnalyticsEventsDao {
    @Query("SELECT * FROM AnalyticsEvent order by timeStamp")
    suspend fun get(): List<AnalyticsEvent>

    @Query("SELECT * FROM AnalyticsEvent WHERE timeStamp BETWEEN :from AND :to order by timeStamp")
    suspend fun get(from: Date, to: Date): List<AnalyticsEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(logEvents: AnalyticsEvent)

    @Query("DELETE FROM AnalyticsEvent")
    suspend fun clear()
}
