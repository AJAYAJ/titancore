package titan.core.products.reflex_3

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*


/*
* 0328AA0301090F1200
* Position0 : Command_Id
* Position1 : Key_Id
* Position2 : on_off(AA) -> open
* Position3 : Show_secs(03) -> range is 2-10sec
* Position4 : Has_time_range(01) -> yes
* Position5 : start_hour(09) -> 24H format
* Position6 : start_minute(0F) -> 15
* position7 : end_hour(12) -> 18 -> 24H format
* position8 : end_minute(00)
* Remaining 11 bytes are reserved
* */

private const val commandId: Byte = 3
private const val keyId: Byte = 40

class R3LiftWristToViewCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null
    fun set(liftWristToView: CoreLiftWristToView): R3LiftWristToViewCommand {
        val data: ByteArray = byteArrayOf(
            commandId,
            keyId,
            if (liftWristToView.isEnabled) -86 else 85,
            liftWristToView.displayDuration.toByte(),
            if (liftWristToView.isTimeRange) 1 else 0,
            liftWristToView.startHour.toByte(),
            liftWristToView.startMin.toByte(),
            liftWristToView.endHour.toByte(),
            liftWristToView.endMin.toByte()
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
        } else if (byteArray.size < 3) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun callback(listener: DataCallback<Boolean>): R3LiftWristToViewCommand {
        this.listener = listener
        return this
    }

    fun parse(byteArray: ByteArray) {
        /*status：（0x00:Successful ，0x01：failed，02：failed：parameter error）*/
        when (byteArray[2].asInt()) {
            0 -> {
                listener?.onResult(Response.Status(true))
            }
            1 -> {
                listener?.onResult(Response.Status(false))
            }
            2 -> {
                /*Parameter Error may be document changed. Need to log the output and firmware version*/
                listener?.onResult(Response.Status(false))
            }
            else -> {
                listener?.onResult(Response.Status(false))
            }
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