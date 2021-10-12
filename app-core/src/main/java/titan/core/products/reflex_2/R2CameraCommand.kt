package titan.core.products.reflex_2

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

private const val command: Byte = -66
private const val id: Byte = 6
private const val key: Byte = 7
private const val endKey: Byte = -34

internal class R2CameraCommand: DataCommand {
    private var listener: DataCallback<Boolean>? = null

    fun cameraAcknowledgement(): R2CameraCommand {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R2_COMMUNICATION_SERVICE,
                    R2_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(command, id, key, -19),
                key = getKey()
            )
        }
        return this
    }

    fun callback(listener: DataCallback<Boolean>): R2CameraCommand {
        this.listener = listener
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && (byteArray[1] != id || byteArray[2] != key)) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 4) {
            // Todo update to callback
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun parse(byteArray: ByteArray) {
        when (byteArray[3].asInt()) {
            237 -> {
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