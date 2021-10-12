package com.titan.log

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [LogEvent::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(LogConverters::class)
abstract class LogDatabase : RoomDatabase() {
    abstract fun log(): LogEventsDao

    companion object {
        val INSTANCE: LogDatabase by lazy {
            Room.databaseBuilder(
                LogApp.application,
                LogDatabase::class.java,
                "log"
            ).build()
        }
    }
}