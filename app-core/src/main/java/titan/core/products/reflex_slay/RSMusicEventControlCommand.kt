package titan.core.products.reflex_slay

import titan.core.products.DataCommand
import titan.core.products.ResponseStatus
import java.util.*

private const val notifyEventMessageLength: Byte = 1
private const val notifyEventKeyMessageId: Byte = 19
private val musicCommands = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

class RSMusicEventControlCommand : DataCommand {

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {

    }

    fun check() {

    }

    fun checkEventControl(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != notifyEventMessageLength) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != notifyEventKeyMessageId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && musicCommands.contains(byteArray[2])) {
            ResponseStatus.COMPLETED
        } else {
            ResponseStatus.INCOMPATIBLE
        }
    }
}