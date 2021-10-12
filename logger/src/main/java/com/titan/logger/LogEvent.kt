package com.titan.logger

import com.titan.log.AppLevel
import com.titan.log.EventType

fun logger(event: String, level: AppLevel, type: EventType = EventType.INFO) {
    com.titan.log.logger(event, level, type)
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