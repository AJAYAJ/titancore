package titan.core.products.reflex_2

import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/*Remarks: When the user clicks OK, the mobile phone starts the 6.31 command, and the device vibrates for 5s after
receiving the command.*/
private const val command: Byte = -66
private const val id: Byte = 6
private const val key: Byte = 15
private const val endKey: Byte = -34

internal class R2FindBandCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null
    fun findBand():R2FindBandCommand {
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

    fun callback(listener: DataCallback<Boolean>): R2FindBandCommand {
        this.listener = listener
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && (byteArray[1] != id || byteArray[2] != key)) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 4) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            listener?.onResult(Response.Status(true))
            ResponseStatus.COMPLETED
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