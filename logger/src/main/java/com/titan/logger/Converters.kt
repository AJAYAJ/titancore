package com.titan.logger

import androidx.databinding.BindingConversion
import androidx.room.TypeConverter
import java.util.*


/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */


class Converters {
    @TypeConverter
    fun timeToDate(value: Long): Date {
        return Date(value)
    }

    @TypeConverter
    fun dateToTime(value: Date): Long {
        return value.time
    }
}