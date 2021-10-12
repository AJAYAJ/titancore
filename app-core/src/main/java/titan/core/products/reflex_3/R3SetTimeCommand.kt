package titan.core.products.reflex_3

import titan.core.bluetooth.CommManager
import titan.core.getLocalCalendar
import titan.core.products.*
import titan.core.toBytes
import java.util.*

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */

private const val commandId: Byte = 3
private const val keyId: Byte = 1

internal class R3SetTimeCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }

    fun set(): R3SetTimeCommand {
        val calendar = getLocalCalendar()
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(
                    commandId, keyId,
                    calendar.get(Calendar.YEAR).toBytes()[1],
                    calendar.get(Calendar.YEAR).toBytes()[0],
                    (calendar.get(Calendar.MONTH) + 1).toByte(),
                    calendar.get(Calendar.DATE).toByte(),
                    calendar.get(Calendar.HOUR_OF_DAY).toByte(),
                    calendar.get(Calendar.MINUTE).toByte(),
                    calendar.get(Calendar.SECOND).toByte(),
                    getDayOfWeek(calendar)
                ),
                key = getKey()
            )
        }
        return this
    }

    private fun getDayOfWeek(calendar: Calendar): Byte {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0.toByte()
            Calendar.TUESDAY -> 1.toByte()
            Calendar.WEDNESDAY -> 2.toByte()
            Calendar.THURSDAY -> 3.toByte()
            Calendar.FRIDAY -> 4.toByte()
            Calendar.SATURDAY -> 5.toByte()
            Calendar.SUNDAY -> 6.toByte()
            else -> 0.toByte()
        }
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != commandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            ResponseStatus.INCOMPATIBLE
        } else {
            listener?.onResult(Response.Status(true))
            ResponseStatus.COMPLETED
        }
    }

    fun callback(listener: DataCallback<Boolean>): R3SetTimeCommand {
        this.listener = listener
        return this
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }
}