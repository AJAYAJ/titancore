package titan.core.products.reflex_3

import android.os.Message
import titan.core.bluetooth.CommManager
import titan.core.products.BaseParser
import titan.core.products.ResponsePackage
import titan.core.products.ResponseStatus
import titan.core.products.TaskPackage

/*    032201
    Position0: Command_Id
    Position1: Key_Id
    Position2: left_right(01) -> right
    Remaining 17 bytes are reserved*/

private const val commandId: Byte = 3
private const val keyId: Byte = 34
const val R3_HAND_WEAR_SETTING = "r3_hand_wear_setting"

internal object R3HandWearCommand {

    fun setData() {
        val status: Status = Status.RIGHT
        val data: ByteArray = byteArrayOf(
            commandId,
            keyId,
            Status.getValue(status)
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
                key = null
            )
        }
    }

    enum class Status {
        LEFT, RIGHT;

        companion object {
            fun getValue(x: Status): Byte {
                return when (x) {
                    LEFT -> 0.toByte()
                    RIGHT -> 1.toByte()
                }
            }
        }
    }

    fun getMessage(): Message {
        val message = Message()
        message.what = 1
        message.obj = ResponsePackage(R3_HAND_WEAR_SETTING, HashMap(parseData))
        parseData.clear()
        return message
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != commandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            ResponseStatus.INCOMPATIBLE
        } else {
            parseData[0] = byteArray
            ResponseStatus.COMPLETED
        }
    }
}

private var parseData = HashMap<Int, ByteArray>()

internal data class R3HandWear(val data: ByteArray) {
    class Parser : BaseParser {
        private lateinit var model: R3HandWear
        override fun parse(data: HashMap<Int, ByteArray>) {
            println(data.toString())
        }
    }
}