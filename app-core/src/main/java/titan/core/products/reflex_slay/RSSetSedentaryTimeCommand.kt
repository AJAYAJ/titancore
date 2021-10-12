package titan.core.products.reflex_slay

import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

private const val messageId: Byte = 49
private const val messageLength: Byte = 5
private const val endKey: Byte = 2
private const val endId: Byte = -1

internal class RSSetSedentaryTimeCommand : DataCommand {
    private var listener: DataCallback<SedentaryReminder>? = null

    fun set(sedentaryReminder: SedentaryReminder): RSSetSedentaryTimeCommand {
        val sedentary: Sedentary = sedentaryReminder.list[0]
        val data: ByteArray = byteArrayOf(
            messageLength,
            messageId,
            sedentary.interval.toByte(),
            sedentary.start.first.toByte(),
            sedentary.start.second.toByte(),
            sedentary.end.first.toByte(),
            sedentary.end.second.toByte()
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
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != endId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 4) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            listener?.onResult(Response.Status(true))
            ResponseStatus.COMPLETED
        }
    }

    fun callback(listener: DataCallback<SedentaryReminder>): RSSetSedentaryTimeCommand {
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