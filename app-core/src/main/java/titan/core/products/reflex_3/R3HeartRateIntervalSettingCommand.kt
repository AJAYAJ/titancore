package titan.core.products.reflex_3

import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/*    0324697EA86E0A0F14191E
    Position0: Command_Id
    Position1: Key_Id
    Position2: burn_fat_threshold(105) -> 69
    Position3 : aerobic_threshold(126) ->7E
    Position4: limit_threshold(168) -> A8
    Position5: User_max_hr(110) ->6E
    Position6: Range1(10)-> 0A
    Position7: Range2(15) -> 0F
    Position8: Range3(20) ->14
    Position9: Range4(25) -> 19
    position10: Range5(30) ->1E
    Remaining 15 bytes are reserved*/

private const val commandId: Byte = 3
private const val keyId: Byte = 36

class R3HeartRateIntervalSettingCommand: DataCommand {
    private var listener: DataCallback<Boolean>? = null

    fun setData(): R3HeartRateIntervalSettingCommand {
        val burnFatThreshold = 105
        val aerobicThreshold = 126
        val limitThreshold = 168
        val userMaxHR = 200
        val range1 = 10
        val range2 = 15
        val range3 = 20
        val range4 = 25
        val range5 = 30
        val data: ByteArray = byteArrayOf(
            commandId,
            keyId,
            burnFatThreshold.toByte(),
            aerobicThreshold.toByte(),
            limitThreshold.toByte(),
            userMaxHR.toByte(),
            range1.toByte(),
            range2.toByte(),
            range3.toByte(),
            range4.toByte(),
            range5.toByte()
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

    fun callback(listener: DataCallback<Boolean>): R3HeartRateIntervalSettingCommand {
        this.listener = listener
        return this
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }

    fun parse(data: ByteArray) {
        println(data.toString())
    }
}
