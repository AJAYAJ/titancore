package titan.core.products.reflex_2

import android.os.Message
import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.BaseParser
import titan.core.products.ResponsePackage
import titan.core.products.ResponseStatus
import titan.core.products.TaskPackage

private const val command: Byte = -66
private const val id: Byte = 1
private const val key: Byte = 11
private const val endKey: Byte = -34
const val R2_SET_BLE_BROADCAST_NAME = "r2_set_ble_broadcast_name"

internal object R2SetBleBroadcastNameCommand {
    // todo
    fun setData() {
        val name: String = "Reflex 2 Beat"
        if (name.length > 15) {
            return
        }
        val data: ByteArray = ByteArray(20)
        data.plus(byteArrayOf(command, id, key, -2).plus(name.toByte()))
        if (data.size != 20) {
            data.fill(-2, data.size - 1, 19)
        }
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

internal data class R2SetBleBroadcastName(var data: ByteArray) {
    class Parser : BaseParser {
        override fun parse(data: HashMap<Int, ByteArray>) {
            data[0]?.let { byteArray ->
                when (byteArray[3].asInt()) {
                    237 -> {
                        /*Set Method Req Response will handle here*/
                        println(data)
                    }
                }
            }
        }

    }
}