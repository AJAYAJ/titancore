package titan.core.products.reflex_3

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.convertToBinaryString
import titan.core.products.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */

private const val commandId: Byte = 2
private const val keyId: Byte = 48

class R3GetDNDSettingsCommand : DataCommand {
    private var listener: DataCallback<CoreDND>? = null

    fun get(): R3GetDNDSettingsCommand {
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

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != commandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 9) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    private fun parse(data:ByteArray){
        val repetitionDays: ArrayList<DaysList> = ArrayList()
        val repeatDaysSwitch = data[8].convertToBinaryString().reversed()
        for (day in 1..7) {
            if (repeatDaysSwitch[day].toString() == "1") {
                when (day) {
                    0 -> {
                        repetitionDays.add(DaysList.SUNDAY)
                    }
                    1 -> {
                        repetitionDays.add(DaysList.MONDAY)
                    }
                    2 -> {
                        repetitionDays.add(DaysList.TUESDAY)
                    }
                    3 -> {
                        repetitionDays.add(DaysList.WEDNESDAY)
                    }
                    4 -> {
                        repetitionDays.add(DaysList.THURSDAY)
                    }
                    5 -> {
                        repetitionDays.add(DaysList.FRIDAY)
                    }
                    6 -> {
                        repetitionDays.add(DaysList.SATURDAY)
                    }
                }
            }
        }
        val model = CoreDND(
            isOn = data[2].asInt()==170,//0xAA on,0x55 off
            start = Pair(data[3].asInt(),data[4].asInt()),
            end = Pair(data[5].asInt(),data[6].asInt()),
            isTimeBased = data[7].asInt() == 2,/*0x00 invalid, 0x01 means no time range, 0x02 means time range*/
            repeatWeekly = repetitionDays.size>0,
            repeatDaysList =repetitionDays
        )
        listener?.onResult(Response.Result(model))
    }

    fun callback(listener: DataCallback<CoreDND>): R3GetDNDSettingsCommand {
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

}