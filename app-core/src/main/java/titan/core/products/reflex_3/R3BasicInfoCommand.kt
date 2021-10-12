package titan.core.products.reflex_3

import com.google.gson.Gson
import com.titan.logger.coreLogger
import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import titan.core.toInt
import java.util.*

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

/**
Position 0: Command Id
Position 1: Key Id
Position 2&3 : Device Id
Position 4: Firmware Version Number
Position 5: Running Mode (
Position 6: Battery Status
Position 7: Energy battery power
Position 8: Pairing Flag or binding flag
Position 9: Reboot or Restart Flag
Position 10: Has Detailed Version (Not Came)

run_mode:running mode(0x00:sports mode, 0x01:sleep mode)
battery_status:battery status (0x00: normal, 0x01:charging,0x02:fully charged, 0x03:low
battery)
pair_flag: binding flag(0x01: Already bound, 0x00:Unbound)
reboot_flag: reboot flag(0x00:No restart, 0x01:Have a reboot)
has_detail_version:detailed version(0x00:NO, 0x01:YES)
 */

private const val commandId: Byte = 2
private const val keyId: Byte = 1

internal class R3BasicInfoCommand : DataCommand {
    private var listener: DataCallback<DeviceInfo>? = null
    fun getData(): R3BasicInfoCommand {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(commandId, keyId),
                key = getKey()
            )
        }
        return this
    }

    fun callback(callback: DataCallback<DeviceInfo>): R3BasicInfoCommand {
        listener = callback
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != commandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 11) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    private fun parse(data: ByteArray) {
        data.let { byteArray ->
            val model = R3DeviceInfo(
                deviceID = byteArrayOf(byteArray[2], byteArray[3]).toInt(true),
                firmwareVersion = byteArray[4].asInt().toString(),
                deviceRunningMode = if (byteArray[5].asInt() == 0) BandMode.SPORTS_MODE else BandMode.SLEEP_MODE,
                batteryStatus = BatteryStatusMode.getMode(byteArray[6].asInt()),
                batteryLevel = byteArray[7].asInt(),
                bindingStatus = if (byteArray[8].asInt() == 1) BindingStatus.BOUNDED else BindingStatus.UN_BOUNDED,
                isBandRebooted = byteArray[9].asInt() == 1,
                hasDetailedVersion = byteArray[10].asInt() == 1
            )
            coreLogger("Device Info:${Gson().toJson(model)}")
            println("Device Info:${Gson().toJson(model)}")
            listener?.onResult(Response.Result(model))
        }
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }
}

data class R3DeviceInfo(
    val deviceID: Int = 0,
    val firmwareVersion: String = "",
    val deviceRunningMode: BandMode = BandMode.NONE,
    val batteryStatus: BatteryStatusMode = BatteryStatusMode.NONE,
    val batteryLevel: Int = 0,
    val bindingStatus: BindingStatus = BindingStatus.NONE,
    val isBandRebooted: Boolean = false,
    val hasDetailedVersion: Boolean = false
) : DeviceInfo