package titan.core

import android.app.Application
import titan.core.bluetooth.CommManager

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

object Core {
    private lateinit var application: Application

    fun init(app: Application) {
        application = app
        CommManager.init(app)
    }

    fun getApplication(): Application {
        return application
    }
}