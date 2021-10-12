package titan.core.products.reflex_slay

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.getLocalCalendar
import titan.core.products.*
import titan.core.toInt
import java.util.*

private const val messageId: Byte = 0
private const val messageLength: Byte = 2
private const val endKey: Byte = 8
private const val endId: Byte = -1

internal class RSGetTimeCommand : DataCommand {
    private var listener: DataCallback<DeviceTime>? = null

    fun get(): RSGetTimeCommand {
        val data = byteArrayOf(
            messageId,
            messageLength
        )
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    RS_COMMUNICATION_SERVICE,
                    RS_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = data,
                key = getKey()
            )
        }
        println(data)
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 10) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    private fun parse(data: ByteArray) {
        val calendar = getLocalCalendar().apply {
            set(Calendar.YEAR, byteArrayOf(data[3], data[4]).toInt())
            set(Calendar.MONTH, data[5].toInt() - 1)
            set(Calendar.DAY_OF_MONTH, data[6].toInt())
            set(Calendar.HOUR_OF_DAY, data[7].toInt())
            set(Calendar.MINUTE, data[8].toInt())
            set(Calendar.SECOND, data[9].toInt())
        }
        listener?.onResult(
            Response.Result(
                DeviceTime.RSDeviceTime(
                    calendar = calendar
                )
            )
        )
    }

    fun callback(listener: DataCallback<DeviceTime>): RSGetTimeCommand {
        this.listener = listener
        return this
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }

}