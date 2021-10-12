package titan.core.products.reflex_3

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */
private const val command: Byte = 8
private const val id: Byte = 1
class R3ActiveDataCountCommand :DataCommand{
    private var listener: DataCallback<R3ActiveDataCount>? = null

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }

    fun getData() : R3ActiveDataCountCommand{
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_HEALTH_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(command, id, 0, 0),
                key = getKey()
            )
        }
        return this
    }

    fun parse(data:ByteArray) {
        data.let { byteArray ->
            listener?.onResult(
                Response.Result(
                    R3ActiveDataCount(
                        steps = byteArray[4].asInt(),
                        sleep = byteArray[5].asInt(),
                        heartRate = byteArray[6].asInt()
                    )
                )
            )
        }
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != command) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != id) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 7) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun callback(listener: DataCallback<R3ActiveDataCount>): R3ActiveDataCountCommand {
        this.listener = listener
        return this
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }
}

data class R3ActiveDataCount(
    val steps:Int = 0,
    val sleep:Int = 0,
    val heartRate:Int = 0
)