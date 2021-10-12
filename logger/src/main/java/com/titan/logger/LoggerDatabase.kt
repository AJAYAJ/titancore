package com.titan.logger

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

@Database(
    entities = [ AnalyticsEvent::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LoggerDatabase : RoomDatabase() {
    abstract fun analytics(): AnalyticsEventsDao

    companion object {
        val INSTANCE: LoggerDatabase by lazy {
            Room.databaseBuilder(
                Logger.application,
                LoggerDatabase::class.java,
                "logger"
            ).build()
        }
    }
}