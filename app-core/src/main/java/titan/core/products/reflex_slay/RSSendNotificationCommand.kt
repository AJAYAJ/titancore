package titan.core.products.reflex_slay

import titan.core.bluetooth.CommManager
import titan.core.byteMerge
import titan.core.products.*
import java.util.*

private const val endKey: Byte = 2
private const val endId: Byte = -1
private const val notificationId: Byte = 16
private var notificationSerialNo: Int = -1

internal class RSSendNotificationCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null

    fun sendNotification(coreNotificationModel: CoreNotificationModel): RSSendNotificationCommand {
        notificationSerialNo = coreNotificationModel.index
        val data: ByteArray = byteMerge(
            byteArrayOf(
                coreNotificationModel.index.toByte(),
                notificationId
            ), coreNotificationModel.data
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

    fun callback(callback: DataCallback<Boolean>): RSSendNotificationCommand {
        listener = callback
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != notificationSerialNo.toByte()) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && (byteArray[1] != endId)) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 3 && (byteArray[2] != notificationId)) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 4) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
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
}