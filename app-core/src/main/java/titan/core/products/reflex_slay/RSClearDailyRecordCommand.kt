package titan.core.products.reflex_slay

import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

private const val messageId: Byte = 65
private const val messageLength: Byte = 1
private const val endKey: Byte = 2
private const val endId: Byte = -1

class RSClearDailyRecordCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null

    fun setData(): RSClearDailyRecordCommand {
        val data: ByteArray = byteArrayOf(
            messageLength,
            messageId,
            1.toByte()
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
        } else {
            listener?.onResult(Response.Status(true))
            ResponseStatus.COMPLETED
        }
    }

    fun callback(listener: DataCallback<Boolean>): RSClearDailyRecordCommand {
        this.listener = listener
        return this
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {
        listener?.onResult(Response.Status(true))
    }

    fun parse(data: ByteArray) {
        println(data.toString())
    }
}