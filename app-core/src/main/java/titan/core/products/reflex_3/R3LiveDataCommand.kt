package titan.core.products.reflex_3

import android.os.Message
import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.BaseParser
import titan.core.products.ResponsePackage
import titan.core.products.ResponseStatus
import titan.core.products.TaskPackage
import titan.core.toLong

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

private const val commandId: Byte = 2
private const val keyId: Byte = -96
const val R3_LIVE_DATA = "r3_live_data"

internal object R3LiveDataCommand {
    fun getData() {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(commandId, keyId, 0),
                key = null
            )
        }
    }

    private var parseData = HashMap<Int, ByteArray>()

    fun getMessage(): Message {
        val message = Message()
        message.what = 1
        message.obj = ResponsePackage(R3_LIVE_DATA, HashMap(parseData))
        parseData.clear()
        return message
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != commandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 19) {
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parseData[0] = byteArray
            ResponseStatus.COMPLETED
        }
    }
}

internal data class R3LiveData(
    val steps: Long,
    val calories: Long,
    val distance: Long,
    val activeTime: Long,
    val hr: Int
) {
    class Parser : BaseParser {
        private lateinit var model: R3LiveData

        override fun parse(data: HashMap<Int, ByteArray>) {
            data[0]?.let { byteArray ->
                model = R3LiveData(
                    steps = byteArrayOf(
                        byteArray[2],
                        byteArray[3],
                        byteArray[4],
                        byteArray[5]
                    ).toLong(),
                    calories = byteArrayOf(
                        byteArray[6],
                        byteArray[7],
                        byteArray[8],
                        byteArray[9]
                    ).toLong(),
                    distance = byteArrayOf(
                        byteArray[10],
                        byteArray[11],
                        byteArray[12],
                        byteArray[13]
                    ).toLong(),
                    activeTime = byteArrayOf(
                        byteArray[14],
                        byteArray[15],
                        byteArray[16],
                        byteArray[17]
                    ).toLong(),
                    hr = byteArray[18].asInt()
                )
                println(model.toString())
            }
        }
    }
}