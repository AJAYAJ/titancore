package titan.core.products.reflex_slay

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

private const val messageId: Byte = 21
private const val messageLength: Byte = 1

internal class RSTakePictureCommand: DataCommand {
    private var listener: DataCallback<Boolean>? = null

    fun cameraAcknowledgement(): RSTakePictureCommand {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    RS_COMMUNICATION_SERVICE,
                    RS_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(messageLength, messageId, 1),
                key = getKey()
            )
        }
        return this
    }

    fun callback(listener: DataCallback<Boolean>): RSTakePictureCommand {
        this.listener = listener
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != messageLength) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != messageId ) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 3) {
            // Todo update to callback
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun parse(byteArray: ByteArray) {
        when (byteArray[2].asInt()) {
            1 -> {
                listener?.onResult(Response.Status(true))
            }
            254 -> {

            }
            else -> {
                listener?.onResult(Response.Status(false))
            }
        }
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }
}