package titan.core.products.reflex_slay

import titan.core.products.DataCommand
import titan.core.products.ResponseStatus
import java.util.*

private const val messageLength: Byte = 1
private const val messageId: Byte = 23
private const val exerciseCompletionCommand: Byte = 1

class RSExerciseCompletionNotifyCommand : DataCommand {

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {

    }

    fun check() {

    }

    fun checkEventControl(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != messageLength) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != messageId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && byteArray[2] == exerciseCompletionCommand) {
            ResponseStatus.COMPLETED
        } else {
            ResponseStatus.INCOMPATIBLE
        }
    }

}