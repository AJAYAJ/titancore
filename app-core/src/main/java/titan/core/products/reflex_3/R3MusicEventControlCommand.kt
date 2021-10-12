package titan.core.products.reflex_3

import titan.core.products.DataCommand
import titan.core.products.ResponseStatus
import java.util.*

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */

private const val notifyEventCommandId: Byte = 7
private const val notifyEventKeyId: Byte = 1
private val musicCommands = byteArrayOf(1, 2, 3, 4, 5, 8, 9)

class R3MusicEventControlCommand : DataCommand {

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {

    }

    fun check() {

    }

    fun checkEventControl(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != notifyEventCommandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != notifyEventKeyId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && musicCommands.contains(byteArray[2])) {
            ResponseStatus.COMPLETED
        } else {
            ResponseStatus.INCOMPATIBLE
        }
    }
}