package titan.core.products.reflex_3

import titan.core.bluetooth.CommManager
import titan.core.products.BindingStatus
import titan.core.products.DataCallback
import titan.core.products.DataCommand
import titan.core.products.TaskPackage
import java.util.*

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

class R3PinPairingEnable : DataCommand {
    private var listener: DataCallback<BindingStatus>? = null

    fun enable(): R3PinPairingEnable {
        val data: ByteArray = byteArrayOf(
            170.toByte(),
            250.toByte(),
            1.toByte()
        )
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = false,
                data = data,
                key = getKey()
            )
        }
        return this
    }

    override fun getKey(): UUID? {
        return null
    }

    override fun failed() {
    }
}