package titan.core.products.reflex_2

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */

private const val command: Byte = -66
private const val id: Byte = 1
private const val key: Byte = 24
private const val endKey: Byte = -34


internal class R2LiftWristToViewCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null

    fun setData(liftWristToView : CoreLiftWristToView): R2LiftWristToViewCommand {
        val data = byteArrayOf(
            command,
            id,
            key,
            -2,
            if (liftWristToView.isEnabled) 1.toByte() else 0.toByte(),
            if (liftWristToView.isEnabled) (if(liftWristToView.isSleepEnabled) 1.toByte() else 0.toByte() ) else 0.toByte()
        )
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R2_COMMUNICATION_SERVICE,
                    R2_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = data,
                key = getKey()
            )
        }
        return this
    }

    fun callback(listener: DataCallback<Boolean>): R2LiftWristToViewCommand {
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