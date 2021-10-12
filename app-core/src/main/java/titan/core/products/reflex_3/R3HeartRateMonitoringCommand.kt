package titan.core.products.reflex_3

import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/*    03258800090F1200
    Position0: Command_Id
    Position1: Key_Id
    Position2: mode(88) -> automatic mode
    Note: 0x55: Temporarily not supported, the default is automatic mode
    Position3 : Has_time_range(00) ->nano
    Position4: start_hour(09) -> 24H format
    Position5: start_minute(15) ->0F
    Position6: end_hour(18)-> 12 (24H format)
    Position7: end_minute(00)
    Position8: end_minute(00)
    Remaining 12 bytes are reserved*/

private const val commandId: Byte = 3
private const val keyId: Byte = 37

class R3HeartRateMonitoringCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null

    fun setData(interval: Int, enable: Boolean): R3HeartRateMonitoringCommand {
        val data: ByteArray = byteArrayOf(
            commandId,
            keyId,
            if (enable) 136.toByte() else 85.toByte(),
            1,
            0,
            0,
            23.toByte(),
            59.toByte(),
            interval.toByte()
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
                key = getKey()
            )
        }
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != commandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            ResponseStatus.INCOMPATIBLE
        } else {
            listener?.onResult(Response.Status(true))
            ResponseStatus.COMPLETED
        }
    }

    fun callback(listener: DataCallback<Boolean>): R3HeartRateMonitoringCommand {
        this.listener = listener
        return this
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {
        listener?.onResult(Response.Status(true))
    }

    fun parse(data: ByteArray) {
        println(data.toString())
    }
}