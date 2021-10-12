package titan.core.products.reflex_3

import android.os.Message
import titan.core.bluetooth.CommManager
import titan.core.products.BaseParser
import titan.core.products.ResponsePackage
import titan.core.products.ResponseStatus
import titan.core.products.TaskPackage


/*    032B02
    Position0: Command_Id
    Position1: Key_Id
    Position2: mode(02) -> vertical screen

    Remaining 17 bytes are reserved*/

private const val commandId: Byte = 3
private const val keyId: Byte = 43
const val R3_DISPLAYMODE_SETTING = "r3_displaymode_setting"

internal object R3DisplaymodeSettingsCommand {

    fun setData() {
        val mode: DisplayMode = DisplayMode.VERTICAL_SCREEN
        val data: ByteArray = byteArrayOf(
            commandId,
            keyId,
            DisplayMode.getValue(mode)
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


    enum class DisplayMode {
        DEFAULT, HORIZONTAL_SCREEN, VERTICAL_SCREEN, FLIP_180_DEGREES;

        companion object {
            fun getValue(x: DisplayMode): Byte {
                return when (x) {
                    DEFAULT -> 0.toByte()
                    HORIZONTAL_SCREEN -> 1.toByte()
                    VERTICAL_SCREEN -> 2.toByte()
                    FLIP_180_DEGREES -> 3.toByte()
                }
            }
        }
    }

    fun getMessage(): Message {
        val message = Message()
        message.what = 1
        message.obj = ResponsePackage(R3_DISPLAYMODE_SETTING, HashMap(parseData))
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

internal data class R3DisplaymodeSettings(val data: ByteArray) {
    class Parser : BaseParser {
        private lateinit var model: R3DisplaymodeSettings
        override fun parse(data: HashMap<Int, ByteArray>) {
            println(data.toString())
        }
    }

}