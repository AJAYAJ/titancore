package titan.core.products.reflex_3

import android.os.Message
import com.google.gson.Gson
import titan.core.bluetooth.CommManager
import titan.core.products.BaseParser
import titan.core.products.ResponsePackage
import titan.core.products.ResponseStatus
import titan.core.products.TaskPackage

/*    03230209
    Position0: Command_Id
    Position1: Key_Id
    Position2: system(02) -> android
    Position3: system_version(09), need to check it
    Remaining 16 bytes are reserved*/

private const val commandId: Byte = 3
private const val keyId: Byte = 35
const val R3_SMARTPHONE_SETTING = "r3_smartphone_setting"

internal object R3SmartphoneSettingsCommand {

    fun setData() {
        val system: Systemtype = Systemtype.ANDROID
        val systemVersion: Int = 9
        val data: ByteArray = byteArrayOf(
            commandId,
            keyId,
            Systemtype.getValue(system),
            systemVersion.toByte()
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

    enum class Systemtype {
        IOS, ANDROID;

        companion object {
            fun getValue(x: Systemtype): Byte {
                return when (x) {
                    IOS -> 1.toByte()
                    ANDROID -> 2.toByte()
                }
            }
        }

    }

    fun getMessage(): Message {
        val message = Message()
        message.what = 1
        message.obj = ResponsePackage(R3_SMARTPHONE_SETTING, HashMap(parseData))
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

internal data class R3SmartphoneSetting(val data: ByteArray) {
    class Parser : BaseParser {
        private lateinit var model: R3SmartphoneSetting
        override fun parse(data: HashMap<Int, ByteArray>) {
            println(data.toString())
        }
    }
}