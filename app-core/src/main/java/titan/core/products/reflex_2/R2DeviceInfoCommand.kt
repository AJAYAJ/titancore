package titan.core.products.reflex_2

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.byteCopy
import titan.core.products.*
import java.util.*

private const val command: Byte = -66
private const val id: Byte = 6
private const val key: Byte = 9
private const val endKey: Byte = -34


internal class R2DeviceInfoCommand : DataCommand {
    private var listener: DataCallback<DeviceInfo>? = null

    fun get(): R2DeviceInfoCommand {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R2_COMMUNICATION_SERVICE,
                    R2_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(command, id, key, -5),
                key = getKey()
            )
        }
        return this
    }

    fun callback(callback: DataCallback<DeviceInfo>): R2DeviceInfoCommand {
        listener = callback
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && (byteArray[1] != id || byteArray[2] != key)) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 20) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun parse(packet: ByteArray) {
        if (packet[3].asInt() == 251) {
            listener?.onResult(
                Response.Result(
                    R2DeviceInfo(
                        modelNumber = byteCopy(packet, 4, 6).toString(Charsets.UTF_8),
                        hardwareVersion = packet[10].toString(),
                        firmwareVersion = "${packet[11].toInt()}.${packet[12].toInt()}",
                        cameraMode = packet[13].toInt() and 0x01 == 1,
                        vibrate = (packet[13].toInt() shr 2) and 0x01 == 1,
                        findPhone = (packet[13].toInt() shr 3) and 0x01 == 1,
                        music = (packet[13].toInt() shr 5) and 0x01 == 1,
                        heartRate = (packet[14].toInt() shr 1) and 0x01 == 1,
                        isLeftHand = (packet[16].toInt() shr 2) and 0x01 == 0,
                        isAntiLostOn = (packet[16].toInt() shr 3) and 0x01 == 1,
                        isPhoneCallAlertOn = (packet[16].toInt() shr 4) and 0x01 == 1,
                        isSMSDisplayOn = when ((packet[16].toInt() shr 5) and 0x03) {
                            2 -> true
                            3 -> true
                            else -> false
                        },
                        realTime = packet[16].toInt() and 0x03 == 3,
                        isSMSAlertOn = (packet[16].toInt() shr 6) and 0x01 == 1,
                        batteryLevel = packet[17].asInt(),
                        setUp = (packet[18].toInt() shr 0) and 0x01 == 1,
                        rawData = packet
                    )
                )
            )
        } else {
            listener?.onResult(Response.Status(false))
        }
    }

    // TODO: clear data and check this again to understand more in depth

    // 14 byte - -74 101101_0 check other position says as 1, 1st position is heart rate
    // 18 byte - 6 00000110 check other position says as 1
    // 19 is for ANCS for iPhones

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {

    }
}

data class R2DeviceInfo(
    val modelNumber: String = "",
    val hardwareVersion: String ="",
    val firmwareVersion: String = "",
    val isLeftHand: Boolean= false,
    val isAntiLostOn: Boolean =false,
    val isPhoneCallAlertOn: Boolean =false,
    val isSMSAlertOn: Boolean =false,
    val isSMSDisplayOn: Boolean = false,
    val setUp: Boolean = false,
    val batteryLevel: Int = 0,
    val cameraMode: Boolean = false,
    val vibrate: Boolean = false,
    val findPhone: Boolean = false,
    val music: Boolean = false,
    val heartRate: Boolean = false,
    val realTime: Boolean= false,
    val rawData: ByteArray
):DeviceInfo