package com.titan.log

import android.app.Application


object LogApp {
    internal lateinit var application: Application

    fun init(app: Application) {
        application = app
    }
}