package titan.core.products.reflex_3

import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */

private const val commandId: Byte = 5
private const val keyId: Byte = 2

class R3CancelNotificationCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null

    fun setData(
        model: CoreNotificationModel
    ): R3CancelNotificationCommand {
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

    fun callback(listener: DataCallback<Boolean>): R3CancelNotificationCommand {
        this.listener = listener
        return this
    }
}