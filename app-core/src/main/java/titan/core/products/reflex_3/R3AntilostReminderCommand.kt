package titan.core.products.reflex_3

import android.os.Message
import titan.core.bluetooth.CommManager
import titan.core.products.BaseParser
import titan.core.products.ResponsePackage
import titan.core.products.ResponseStatus
import titan.core.products.TaskPackage

/*    032101
    Position0: Command_Id
    Position1: Key_Id
    Position2: mode(01) -> short distance anti lost*/

private const val commandId: Byte = 3
private const val keyId: Byte = 33
const val R3_ANTILOST_REMINDER_SETTING = "r3_antilost_reminder_setting"

internal object R3AntilostReminderCommand {
    fun setData() {

        val mode: Mode = Mode.MIDDLE_RANGE_DISTANCE_ANTI_LOST

        val data: ByteArray = byteArrayOf(
            commandId,
            keyId,
            Mode.getValue(mode)

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


    enum class Mode {
        NO_ANTI_LOST, SHORT_DISTANCE_ANTI_LOST, MIDDLE_RANGE_DISTANCE_ANTI_LOST, LONG_DISTANCE_ANTI_LOST;

        companion object {
            fun getValue(x: Mode): Byte {
                return when (x) {
                    NO_ANTI_LOST -> 0.toByte()
                    SHORT_DISTANCE_ANTI_LOST -> 1.toByte()
                    MIDDLE_RANGE_DISTANCE_ANTI_LOST -> 2.toByte()
                    LONG_DISTANCE_ANTI_LOST -> 3.toByte()
                }
            }
        }

    }

    fun getMessage(): Message {
        val message = Message()
        message.what = 1
        message.obj = ResponsePackage(R3_ANTILOST_REMINDER_SETTING, HashMap(parseData))
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

internal data class R3AntilostReminder(val data: ByteArray) {
    class Parser : BaseParser {
        private lateinit var model: R3AntilostReminder
        override fun parse(data: HashMap<Int, ByteArray>) {
            println(data.toString())
        }
    }
}