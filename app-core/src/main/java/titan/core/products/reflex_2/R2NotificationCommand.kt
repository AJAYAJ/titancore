package titan.core.products.reflex_2

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.byteCopy
import titan.core.byteMerge
import titan.core.products.*
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.ceil

/**
 * Created by Sai Vinay Inabathina
 * Titan Company Ltd
 */

private const val command: Byte = -66
private const val id: Byte = 6
private val keys: Pair<Byte, Byte> = Pair(18, 28)/*Keys Starting Range and end range. Keys a*/
private const val endKey: Byte = -34

internal class R2NotificationCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null

    /*
    *
    * For Every packet, We will send five default values which are [id,key, type, 0xFE, packet-index]
    * */

    fun sendNotification(coreNotificationModel: CoreNotificationModel): R2NotificationCommand {
        val data: ByteArray = byteMerge(
            byteArrayOf(
                command,
                id,
                coreNotificationModel.app.toByte(),
                -2,
                coreNotificationModel.index.toByte()
            ), coreNotificationModel.data
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

    fun callback(callback: DataCallback<Boolean>): R2NotificationCommand {
        listener = callback
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && (byteArray[1] != id)) {
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