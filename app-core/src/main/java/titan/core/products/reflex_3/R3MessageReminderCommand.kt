package titan.core.products.reflex_3

import titan.core.bluetooth.CommManager
import titan.core.byteMerge
import titan.core.products.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList
import kotlin.math.ceil

private const val commandId: Byte = 5
private const val keyId: Byte = 3

class R3MessageReminderCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null

    fun setSMSData(
        model: CoreNotificationModel
    ): R3MessageReminderCommand {
        val data: ByteArray = byteArrayOf(
            commandId,
            keyId
        ).plus(model.data)
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(R3_COMMUNICATION_SERVICE, R3_COMMUNICATION_WRITE_CHAR),
                read = null,
                responseWillNotify = true,
                data = data,
                key = getKey()
            )
        }
        return this
    }

    fun setData(
        model: CoreNotificationModel
    ): R3MessageReminderCommand {
        val data: ByteArray = byteArrayOf(
            commandId,
            keyId
        ).plus(model.data)
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(R3_COMMUNICATION_SERVICE, R3_COMMUNICATION_WRITE_CHAR),
                read = null,
                responseWillNotify = false,
                data = data,
                key = null
            )
        }
        return this
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

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }

    fun callback(listener: DataCallback<Boolean>): R3MessageReminderCommand {
        this.listener = listener
        return this
    }
}