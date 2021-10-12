package titan.core.products.reflex_slay

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.byteCopy
import titan.core.convertToBinaryString
import titan.core.products.*
import titan.core.products.reflex_3.*
import titan.core.toInt
import java.util.*

/*Response data:
0F-FF-01-00-00-07-00-07-01-56-31-2E-30-31-2E-30-32
Index: 3, Size: 2, Type: Uint16, Range: 0-65535,
Additional Info: piece of status data per second up to 18 hours.
Index: 5, Size: 1, Type: Uint8, Range: 0-0xFF,
Additional Info: How many days of 24H data are there altogether, 0 to 15 days.
Index: 6, Size: 1, Type: Uint8, Range: 0-0xFF,
Additional Info: 0 - HR not enabled,
1 - HR measuring, watch not worn on the wrist ,
2 - HR measuring, watch worn on the wrist ,
3 - charging .
Index: 7, Size: 1, Type: Uint8, Additional Info: Hardware status
BIT0: 0 - OHR sensor      Normal 1: OHR sensor error
BIT1: 0 - G sensor           Normal 1: G sensor error
BIT2: 0 – Flash                Normal 1: Flash error
Hardware status normal - 07(111).
Index: 8, Size: 1, Type: Char,
Additional Info: Hardware version No.: ‘0’-‘9’，‘A’-‘B’.
Index: 9, Size: 8, Type: Char[8], Additional Info: Software version No.: 8-digit
ASCII characters - XX.XX.XX (V1.01.02)
56-31-2E-30-31-2E-30-32 */

private const val messageId: Byte = 1
private const val messageLength: Byte = 0
private const val endkey: Byte = 15
private const val endId: Byte = -1

internal class RSDeviceStatusCommand : DataCommand {
    private var listener: DataCallback<DeviceInfo>? = null

    fun getData(): RSDeviceStatusCommand {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    RS_COMMUNICATION_SERVICE,
                    RS_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(messageLength, messageId),
                key = getKey()
            )
        }
        return this
    }

    fun callback(callback: DataCallback<DeviceInfo>): RSDeviceStatusCommand {
        listener = callback
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endkey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != endId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 17) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    private fun parse(data: ByteArray) {
        data.let { byteArray ->
            listener?.onResult(
                Response.Result(
                    RSDeviceStatus(
                        statusData = byteArrayOf(byteArray[3], byteArray[4]).toInt(false),
                        numberOfDaysData = byteArray[5].toInt(),
                        hrStatus = HRStatus.getMode(byteArray[6].asInt()),
                        hardwareStatus = getHardwareStatus(byteArray[7]),
                        hardwareVersion = byteArray[8].asInt().toString(),
                        softwareVersion = byteCopy(data, 9, 8).toString(Charsets.UTF_8)
                    )
                )
            )
        }
    }

    private fun getHardwareStatus(byte: Byte): HardwareStatus {
        val binary = byte.convertToBinaryString().padStart(3, '0').reversed()
        return HardwareStatus(
            ohrSensor = binary[0] == '1',
            gSensor = binary[1] == '1',
            flash = binary[2] == '1'
        )
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }
}

data class RSDeviceStatus(
    val statusData: Int = 0,
    val numberOfDaysData: Int = 0,
    val hrStatus: HRStatus = HRStatus.NONE,
    val hardwareStatus: HardwareStatus,
    val hardwareVersion: String = "",
    val softwareVersion: String = ""
) : DeviceInfo

data class HardwareStatus(
    val ohrSensor: Boolean,
    val gSensor: Boolean,
    val flash: Boolean
)