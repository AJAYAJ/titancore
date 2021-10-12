package titan.core.products.reflex_3

import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/*
* To Send data:
0311020202460201580101
Position0: Command_Id
Position1: Key_Id
Position2: dist_unit(02) -> mi
Position3 : Weight_unit (02) ->lb
Position4: temp_unit(02) -> *F
Position5: Stride_walk (46) -> 70 in cm
Position6: lang(02) -> english
Position7: time_mode(01) -> 24H
Position8: Stride_run(58) -> 88 in cm
Position9: Stride_GPS_calibration_switch(01) -> ON
Position10 : Week Start Day
Remaining 10 bytes are reserved */

private const val commandId: Byte = 3
private const val keyId: Byte = 17

class R3UnitCommand : DataCommand {
    private var listener: DataCallback<UserInfo>? = null
    fun setData(userInfo: UserInfo.R3UserInfo): R3UnitCommand {
        val metric = (if (userInfo.isMetric) 1 else 2).toByte()
        val hourFormat = (if (userInfo.is24HourFormat) 1 else 2).toByte()
        val data = byteArrayOf(
            commandId, keyId, metric,
            metric,
            metric,
            userInfo.getStrideLength().toByte(),
            2,/*language*/
            hourFormat,
            userInfo.getRunLength().toByte(),
            0, 1
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

    fun callback(listener: DataCallback<UserInfo>): R3UnitCommand {
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