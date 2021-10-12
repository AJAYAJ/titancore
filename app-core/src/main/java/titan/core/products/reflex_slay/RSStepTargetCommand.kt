package titan.core.products.reflex_slay

import titan.core.bluetooth.CommManager
import titan.core.products.*
import titan.core.to32BitByte
import java.util.*

private const val messageId: Byte = 56
private const val messageLength: Byte = 3
private const val endKey: Byte = 2
private const val endId: Byte = -1

internal class RSStepTargetCommand : DataCommand {
    private var listener: DataCallback<RSStepTarget>? = null

    fun get(): RSStepTargetCommand {
        val data = byteArrayOf(
            0,
            12
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

    fun setData(stepsTarget: RSStepTarget): RSStepTargetCommand {
        val targetSteps = stepsTarget.stepsTarget.to32BitByte()

        val data: ByteArray = byteArrayOf(
            messageLength,
            messageId,
            targetSteps[3],
            targetSteps[2],
            targetSteps[1]
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

    fun callback(listener: DataCallback<RSStepTarget>): RSStepTargetCommand {
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
            parse(data = byteArray)
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

    fun parse(data: ByteArray) {
        listener?.onResult(
            Response.Status(true)
        )
    }
}

data class RSStepTarget(
    val stepsTarget: Long
)