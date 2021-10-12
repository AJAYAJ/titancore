package titan.core.products.reflex_2

import android.os.Message
import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.BaseParser
import titan.core.products.ResponsePackage
import titan.core.products.ResponseStatus
import titan.core.products.TaskPackage

private const val command: Byte = -66
private const val id: Byte = 6
private const val key: Byte = 4
private const val endKey: Byte = -34
const val R2_UNREAD_SMS_COUNT = "r2_unread_sms_count"
// todo
internal object R2UnreadSMSCountCommand{
    fun setUnreadSMSCount(){
        val unreadSMSCount = 0
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R2_COMMUNICATION_SERVICE,
                    R2_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(command,id, key,-2,unreadSMSCount.toByte()),
                key = null
            )
        }
    }

    fun getMessage(): Message {
        val message = Message()
        message.what = 1
        message.obj = ResponsePackage(R2_CALL_REMINDER, HashMap(parseData))
        parseData.clear()
        return message
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && (byteArray[1] != id || byteArray[2] != key)) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 4) {
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parseData[0] = byteArray
            ResponseStatus.COMPLETED
        }
    }
}

private var parseData = HashMap<Int, ByteArray>()

internal data class R2UnreadSMSCount(var data: ByteArray) {
    class Parser : BaseParser {
        override fun parse(data: HashMap<Int, ByteArray>) {
            data[0]?.let { byteArray ->
                when (byteArray[3].asInt()) {
                    237 -> {
                        /*Set Method Req Response will handle here*/
                        println(data.toString())
                    }
                }
            }
        }

    }
}