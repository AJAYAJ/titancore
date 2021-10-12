package titan.core.products.reflex_slay

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

private const val messageId: Byte = 27
private const val messageLength: Byte = 1
private const val endKey: Byte = 2
private const val endId: Byte = -1

internal class RSDialSelectionCommand : DataCommand {
    private var listener: DataCallback<RSDialSelection>? = null

    fun setData(dialSelection: RSDialSelection): RSDialSelectionCommand {
        val dialSelection: ClockfaceId = ClockfaceId.FACE1
        val data: ByteArray = byteArrayOf(
            messageLength,
            messageId,
            ClockfaceId.getValue(dialSelection)
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

    fun callback(listener: DataCallback<RSDialSelection>): RSDialSelectionCommand {
        this.listener = listener
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
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun parse(byteArray: ByteArray) {
        when (byteArray[2].asInt()) {
            27 -> {
                /*Set Method Req Response will handle here*/
                listener?.onResult(Response.Status(true))
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

data class RSDialSelection(
    val dialSelection: ClockfaceId = ClockfaceId.FACE1
)