package titan.core.products.reflex_2

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.byteMerge
import titan.core.getLocalCalendar
import titan.core.products.*
import java.util.*

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */

private const val command: Byte = -66
private const val id: Byte = 3
private const val key: Byte = 9
private const val endKey: Byte = -34

internal class R2SetDeviceInfoCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null

    fun setDeviceInfo(byteArray: ByteArray): R2SetDeviceInfoCommand {
        val req = ByteArray(20)
        var index = 0
        req[index++] = command
        req[index++] = id
        req[index++] = key
        req[index++] = -2
        req[index++] = byteArray[0]
        req[index++] = byteArray[1]
        req[index++] = byteArray[2]
        req[index++] = byteArray[3]
        req[index] = byteArray[4]
        req[19] = -19
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R2_COMMUNICATION_SERVICE,
                    R2_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = req,
                key = getKey()
            )
        }
        return this
    }

    fun callback(listener: DataCallback<Boolean>): R2SetDeviceInfoCommand {
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