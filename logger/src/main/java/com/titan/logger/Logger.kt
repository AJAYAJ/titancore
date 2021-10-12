package com.titan.logger

import android.app.Application

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

object Logger {
    lateinit var application: Application

    fun init(app: Application) {
        application = app
    }
}