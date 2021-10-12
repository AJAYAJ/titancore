package com.titan.log

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.*

@Dao
interface LogEventsDao {
    @Query("SELECT * FROM LogEvent")
    suspend fun get(): List<LogEvent>

    @Query("SELECT count(*) FROM LogEvent WHERE timeStamp > :afterTime order by timeStamp")
    suspend fun getCount(afterTime: Date): Long

    @Query("SELECT * FROM LogEvent WHERE timeStamp > :afterTime order by timeStamp")
    suspend fun get(afterTime: Date): List<LogEvent>

    @Query("SELECT * FROM LogEvent LIMIT :limit OFFSET :offset")
    suspend fun get(limit: Long, offset: Long): List<LogEvent>

    @Query("SELECT * FROM LogEvent WHERE id > :position order by timeStamp")
    suspend fun get(position: Long): List<LogEvent>

    @Query("SELECT * FROM LogEvent WHERE level = :level order by timeStamp")
    suspend fun get(level: String): List<LogEvent>

    @Query("SELECT * FROM LogEvent WHERE id > :position AND level = :level order by timeStamp")
    suspend fun get(position: Long, level: String): List<LogEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(logEvent: LogEvent)

    @Query("DELETE FROM LogEvent")
    suspend fun clear()

    @Query("DELETE FROM LogEvent WHERE timeStamp < :beforeTime")
    suspend fun clearDataOlderThanDate(beforeTime: Date)
}