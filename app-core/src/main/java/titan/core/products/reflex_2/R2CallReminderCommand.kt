package titan.core.products.reflex_2

import titan.core.bluetooth.CommManager
import titan.core.byteCopy
import titan.core.products.*
import java.util.*

private const val command: Byte = -66
private const val id: Byte = 6
private const val key: Byte = 2
private const val endKey: Byte = -34
const val R2_CALL_REMINDER = "r2_call_reminder"
private const val notifyEventCommandId: Byte = -34
private const val notifyEventKeyId: Byte = 8
private const val notifyEventKey: Byte = 2
private val callNotifyEventCommands = byteArrayOf(-2,1,-19)

class R2CallReminderCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null

    fun setIncomingCallData(model: CoreNotificationModel): R2CallReminderCommand {
        var phoneNumber = model.data
        if (phoneNumber.size > 15) {
            phoneNumber = byteCopy(phoneNumber, 0, 15)
        }
        val data: ByteArray = byteArrayOf(
            command,
            id,
            model.app.toByte(),
            -2,
            phoneNumber.size.toByte()
        ).plus(phoneNumber)
        val emptyData = ByteArray(20)
        System.arraycopy(data,0,emptyData,0,data.size)
        for(i in data.size until 20){
            emptyData[i] = -2
        }
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R2_COMMUNICATION_SERVICE,
                    R2_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = emptyData,
                key = null
            )
        }
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != id) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && byteArray[2] != key) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 4) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            listener?.onResult(Response.Status(true))
            ResponseStatus.COMPLETED
        }
    }

    fun checkEventControl(byteArray: ByteArray): ResponseStatus {

        return if (byteArray.isNotEmpty() && byteArray[0] != notifyEventCommandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != notifyEventKeyId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && byteArray[2] != notifyEventKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size >= 6 && callNotifyEventCommands.contentEquals(byteCopy(byteArray,3,3))) {
            ResponseStatus.COMPLETED
        } else {
            ResponseStatus.INVALID_DATA_LENGTH
        }
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }

    fun callback(listener: DataCallback<Boolean>): R2CallReminderCommand {
        this.listener = listener
        return this
    }
}