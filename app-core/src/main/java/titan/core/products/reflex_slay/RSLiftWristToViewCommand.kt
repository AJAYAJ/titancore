package titan.core.products.reflex_slay

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

private const val messageId: Byte = 55
private const val messageLength: Byte = 5
private const val endKey: Byte = 2
private const val endId: Byte = -1

internal class RSLiftWristToViewCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null
    fun setData(liftWristToView: CoreLiftWristToView): RSLiftWristToViewCommand {
        val data = byteArrayOf(
            messageLength,
            messageId,
            if (liftWristToView.isEnabled) 1.toByte() else 0.toByte(),
            liftWristToView.startHour.toByte(),
            liftWristToView.startMin.toByte(),
            liftWristToView.endHour.toByte(),
            liftWristToView.endMin.toByte()
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

    fun callback(listener: DataCallback<Boolean>): RSLiftWristToViewCommand {
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
            55 -> {
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