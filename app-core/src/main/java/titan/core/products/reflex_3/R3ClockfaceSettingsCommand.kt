package titan.core.products.reflex_3

import android.os.Message
import titan.core.bluetooth.CommManager
import titan.core.products.BaseParser
import titan.core.products.ResponsePackage
import titan.core.products.ResponseStatus
import titan.core.products.TaskPackage


private const val commandId: Byte = 3
private const val keyId: Byte = 18
const val R3_CLOCKFACE_SETTING = "r3_clockface_setting"

internal object R3ClockfaceSettingsCommand {
    fun setData() {
        val faceId: ClockfaceId = ClockfaceId.FACE1
        val data: ByteArray = byteArrayOf(
            commandId,
            keyId,
            ClockfaceId.getValue(faceId)
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


    enum class ClockfaceId {
        INVALID, FACE1, FACE2, FACE3, FACE4;

        companion object {
            fun getValue(x: ClockfaceId): Byte {
                return when (x) {
                    INVALID -> 0.toByte()
                    FACE1 -> 1.toByte()
                    FACE2 -> 2.toByte()
                    FACE3 -> 3.toByte()
                    FACE4 -> 4.toByte()
                }
            }
        }

    }

    fun getMessage(): Message {
        val message = Message()
        message.what = 1
        message.obj = ResponsePackage(R3_CLOCKFACE_SETTING, HashMap(parseData))
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

internal data class R3ClockfaceSettings(val data: ByteArray) {
    class Parser : BaseParser {
        private lateinit var model: R3ClockfaceSettings
        override fun parse(data: HashMap<Int, ByteArray>) {
            println(data.toString())
        }
    }

}