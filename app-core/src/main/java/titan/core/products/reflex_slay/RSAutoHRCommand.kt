package titan.core.products.reflex_slay

import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

private const val messageId: Byte = 26
private const val messageLength: Byte = 1
private const val endKey: Byte = 3
private const val endId: Byte = -1

internal class RSAutoHRCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null

    fun setData(
        detectHRAutomatically: Boolean
    ): RSAutoHRCommand {
        val data: ByteArray = byteArrayOf(
            messageLength,
            messageId,
            if(detectHRAutomatically) 1 else 0
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

    fun callback(listener: DataCallback<Boolean>): RSAutoHRCommand {
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