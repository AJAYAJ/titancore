package titan.core.products.reflex_3

import android.os.Message
import com.google.gson.Gson
import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.BaseParser
import titan.core.products.ResponsePackage
import titan.core.products.ResponseStatus
import titan.core.products.TaskPackage
import titan.core.toInt
import titan.core.toLong

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */


/*
private const val commandId: Byte = 2
private const val keyId: Byte = 5
const val R3_BATTERY_STATUS = "r3_battery_status"

internal object R3BatteryStatusCommand {
    fun getData() {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(commandId, keyId),
                key = null
            )
        }
    }

    private var parseData = HashMap<Int, ByteArray>()

    fun getMessage(): Message {
        val message = Message()
        message.what = 1
        message.obj = ResponsePackage(R3_BATTERY_STATUS, HashMap(parseData))
        parseData.clear()
        return message
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != commandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 15) {
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parseData[0] = byteArray
            ResponseStatus.COMPLETED
        }
    }
}

internal data class R3BatteryStatus(
    val voltage: Int,
    val batteryStatus: BatteryStatus,
    val batteryPower: Int,
    val currentUsedMinutes: Long,
    val totalUsedMinutes: Long
) {
    class Parser : BaseParser {
        private lateinit var model: R3BatteryStatus

        override fun parse(data: HashMap<Int, ByteArray>) {
            data[0]?.let { byteArray ->
                model = R3BatteryStatus(
                    voltage = byteArrayOf(byteArray[3], byteArray[4]).toInt(false),
                    batteryStatus = BatteryStatus.fromInt(byteArray[5].asInt()),
                    batteryPower = byteArray[6].asInt(),
                    currentUsedMinutes = byteArrayOf(
                        byteArray[7],
                        byteArray[8],
                        byteArray[9],
                        byteArray[10]
                    ).toLong() / (60),
                    totalUsedMinutes = byteArrayOf(
                        byteArray[11],
                        byteArray[12],
                        byteArray[13],
                        byteArray[14]
                    ).toLong() / (60)
                )
//                SharedPreference.batteryStatus = model.batteryPower
//                Paper.book().write(R3_BATTERY_STATUS, model)
                println(Gson().toJson(model))
            }
        }
    }

    override fun toString(): String {
        return Gson().toJson(this)
    }
}*/
