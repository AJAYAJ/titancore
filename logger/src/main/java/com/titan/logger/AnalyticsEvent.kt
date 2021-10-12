package com.titan.logger

import androidx.room.Entity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

@Entity(primaryKeys = ["timeStamp"])
data class AnalyticsEvent(
    val timeStamp: Date,
    val event: String,
    val duration: Long
)

var loggerEnableForClickEvents = true

fun analytics(event: String, duration: Int = 0) {
//    if (loggerEnableForClickEvents){
//        CoroutineScope(Dispatchers.IO).launch {
//            LoggerDatabase.INSTANCE.analytics()
//                .insert(AnalyticsEvent(event = event))
//        }
//    }
}

fun duration(timestamp: Long, event: String, duration: Long = 0) {
    if (loggerEnableForClickEvents){
        CoroutineScope(Dispatchers.IO).launch {
            LoggerDatabase.INSTANCE.analytics()
                .insert(
                    AnalyticsEvent(
                        timeStamp = Date((timestamp / 1000) * 1000),
                        event = event,
                        duration = duration
                    )
                )
        }
    }
}