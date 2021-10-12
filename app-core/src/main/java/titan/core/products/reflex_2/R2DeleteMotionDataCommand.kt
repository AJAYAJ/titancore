package titan.core.products.reflex_2

import android.os.Message
import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.BaseParser
import titan.core.products.ResponsePackage
import titan.core.products.ResponseStatus
import titan.core.products.TaskPackage

/*
BE-02-02-FE-E3-07-01-01-00-00
*/
private const val command: Byte = -66
private const val id: Byte = 2
private const val key: Byte = 3
private const val endKey: Byte = -34
const val R2_DELETE_MOTIONDATA = "r2_delete_motiondata"

internal object R2DeleteMotionDataCommand {
// todo
    fun setData() {
        val startYear: Int = 0
        val startMonth: Int = 0
        val startDay: Int = 0
        val timeSerialNumber: Int = 0
        var data = byteArrayOf(
            command,
            id,
            key,
            -2,
            (startYear shr 8).toByte(),
            startYear.toByte(),
            startMonth.toByte(),
            startDay.toByte(),
            (timeSerialNumber shr 8).toByte(),
            timeSerialNumber.toByte()
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
                key = null
            )
        }
    }

    fun getMessage(): Message {
        val message = Message()
        message.what = 1
        message.obj = ResponsePackage(R2_DELETE_MOTIONDATA, HashMap(parseData))
        parseData.clear()
        return message
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && byteArray[2] != key) {
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

internal data class R2DeleteMotionData(var byteArray: ByteArray) {
    class Parser : BaseParser {
        override fun parse(data: HashMap<Int, ByteArray>) {
            data[0]?.let { byteArray ->
                when (byteArray[3].asInt()) {
                    237 -> {
                        println(data.toString())
                    }

                }
            }
        }
    }
}