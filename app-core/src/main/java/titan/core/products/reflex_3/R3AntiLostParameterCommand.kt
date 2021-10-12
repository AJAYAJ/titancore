package titan.core.products.reflex_3

import android.os.Message
import titan.core.bluetooth.CommManager
import titan.core.products.BaseParser
import titan.core.products.ResponsePackage
import titan.core.products.ResponseStatus
import titan.core.products.TaskPackage
import titan.core.toDecimal


/*    034001381E001ED5
    Position0: Command_Id
    Position1: Key_Id
    Position2: mode(01) -> short distance anti lost
    Position3 : RSS(38) ->56 (signal value)
    Position4: anti_delay(1E) -> 30 sec-> 24H format
    Position5: is_disconnect_anti(00) -> no need
    Position6: anti_ disconnect_delay(1E)-> 30 sec
    Position7: repetitions(D5) -> 11010101
    Remaining 12 bytes are reserved*/


private const val commandId: Byte = 3
private const val keyId: Byte = 64
const val R3_ANTI_LOST_PARAMETER_SETTING = "r3_anti_lost_parameter_setting"

internal object R3AntiLostParameterCommand {

    fun setData() {
        val mode: Mode = Mode.SHORT_DISTANCE_ANTI_LOST
        val rss: Int = 56
        val anti_delay: Int = 30
        val is_disconnect_anti: Boolean = false
        val anti_disconnect_delay: Int = 30
        val isRepeat: Boolean = false
        val repetition: ArrayList<DaysList> = ArrayList()
        val data: ByteArray = byteArrayOf(
            commandId,
            keyId,
            Mode.getValue(mode),
            rss.toByte(),
            anti_delay.toByte(),
            antilostDisconnect(is_disconnect_anti),
            anti_disconnect_delay.toByte(),
            repititionDays(isRepeat, repetition)
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

    private fun antilostDisconnect(isDisconnect: Boolean): Byte {
        var need = ""
        need = if (isDisconnect) {
            need.plus("1")
        } else {
            need.plus("0")
        }
        return need.toDecimal().toByte()
    }

    private fun repititionDays(isRepeat: Boolean, repetitions: ArrayList<DaysList>): Byte {
        var repeatDaysString = ""
        repeatDaysString = if (isRepeat) {
            repeatDaysString.plus("1")
        } else {
            repeatDaysString.plus("0")
        }
        enumValues<DaysList>().forEach {
            repeatDaysString = if (repetitions.contains(it)) {
                repeatDaysString.plus("1")
            } else {
                repeatDaysString.plus("0")
            }
        }
        return repeatDaysString.reversed().toDecimal().toByte()
    }

    enum class DaysList {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
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
        message.obj = ResponsePackage(R3_ANTI_LOST_PARAMETER_SETTING, HashMap(parseData))
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

internal data class R3AntilostParameter(val data: ByteArray) {
    class Parser : BaseParser {
        private lateinit var model: R3AntilostParameter
        override fun parse(data: HashMap<Int, ByteArray>) {
            println(data.toString())
        }
    }

}

