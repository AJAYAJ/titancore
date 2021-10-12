package com.titan.log

import androidx.room.TypeConverter
import java.util.*

internal class LogConverters {
    @TypeConverter
    fun timeToDate(value: Long): Date {
        return Date(value)
    }

    @TypeConverter
    fun dateToTime(value: Date): Long {
        return value.time
    }
}