package com.titan.log

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Entity
data class LogEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val timeStamp: Date,
    val event: String,
    val level: String,
    val type: Int
) {
    constructor(event: String, level: String, type: Int) : this(0, Date(), event, level, type)

    override fun toString(): String {
        return "${EventType.getEventType(type)}: ${AppLevel.getAppLevel(level)}: ${
            dateFormat().format(
                timeStamp
            )
        }: $event"
    }
}

var loggerEnable = false

private fun dateFormat(): SimpleDateFormat =
    SimpleDateFormat("dd-MM-yy H:mm:ss SSS", Locale.getDefault())

enum class EventType(val value: Int) {
    INFO(0),
    WARNING(1),
    ERROR(2), ;

    companion object {
        fun getEventType(type: Int): EventType? {
            values().forEach {
                if (type == it.value) {
                    return it
                }
            }
            return null
        }
    }
}

enum class AppLevel(val value: String) {
    APP("App"),
    CORE("Core"),
    BLE("BLE"),
    DFU("DFU"),
    ;

    companion object {
        fun getAppLevel(level: String): AppLevel? {
            values().forEach {
                if (level == it.value) {
                    return it
                }
            }
            return null
        }
    }
}

fun logger(event: String, level: AppLevel, type: EventType = EventType.INFO) {
    if (loggerEnable) {
        CoroutineScope(Dispatchers.IO).launch {
            LogDatabase.INSTANCE.log()
                .insert(LogEvent(event = event, level = level.value, type = type.value))
        }
    }
}

fun appLogger(event: String, type: EventType = EventType.INFO) {
    logger(event, level = AppLevel.APP, type)
}

fun coreLogger(event: String, type: EventType = EventType.INFO) {
    logger(event, level = AppLevel.CORE, type)
}

fun bleLogger(event: String, type: EventType = EventType.INFO) {
    logger(event, level = AppLevel.BLE, type)
}

fun dfuLogger(event: String, type: EventType = EventType.INFO) {
    logger(event, level = AppLevel.DFU, type)
}