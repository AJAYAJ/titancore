package titan.core.products.reflex_2

import titan.core.products.DataCommand
import titan.core.products.ResponseStatus
import java.util.*

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

private val musicCommands =
    byteArrayOf(0.toByte(), 1.toByte(), 2.toByte(), 3.toByte(), 240.toByte())

class R2MusicEventControlCommand : DataCommand {

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {

    }

    fun check() {

    }

    fun checkEventControl(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.size != 6 || byteArray[0] != 222.toByte() ||
            byteArray[1] != 6.toByte() ||
            byteArray[2] != 8.toByte() ||
            byteArray[3] != 254.toByte() ||
            byteArray[5] != 237.toByte()
        ) {
            ResponseStatus.INCOMPATIBLE
        } else if (musicCommands.contains(byteArray[4])) {
            ResponseStatus.COMPLETED
        } else {
            ResponseStatus.INCOMPATIBLE
        }
    }
}