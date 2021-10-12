package titan.core.products.reflex_3

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*


/*    0326AA
    Position0: Command_Id
    Position1: Key_Id
    Position2: on_off(AA) -> Open
    Remaining 17 bytes are reserved*/

private const val commandId: Byte = 3
private const val keyId: Byte = 38

private const val notifyEventCommandId: Byte = 7
private const val notifyEventKeyId: Byte = 2
private val findPhoneActionCommands = byteArrayOf(0, 1)

class R3FindPhoneCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null
    fun setData(findPhone: Boolean): R3FindPhoneCommand {
        val data: ByteArray = byteArrayOf(
            commandId,
            keyId,
            if (findPhone) 170.toByte() else 85,
            30
        )
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_COMMUNICATION_WRITE_CHAR
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
        return if (byteArray.isNotEmpty() && byteArray[0] != commandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            ResponseStatus.INCOMPATIBLE
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }
// TODO
    /*Expected length is 07 02 00/01 0F(Timeout unit seconds)*/
    fun checkEventControl(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != notifyEventCommandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != notifyEventKeyId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && findPhoneActionCommands.contains(byteArray[2])) {
            ResponseStatus.COMPLETED
        } else {
            ResponseStatus.INVALID_DATA_LENGTH
        }
    }

    fun getFindPhoneEvent(byteArray: ByteArray) {
        if (byteArray.size > 2) {
            if (byteArray[2].asInt() == 0) {
                "START"
            } else {
                "STOP"
            }
        } else {

        }
    }

    fun callback(listener: DataCallback<Boolean>): R3FindPhoneCommand {
        this.listener = listener
        return this
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }

    fun parse(data: ByteArray) {
        listener?.onResult(Response.Status(true))
    }
}