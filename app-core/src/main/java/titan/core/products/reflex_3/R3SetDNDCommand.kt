package titan.core.products.reflex_3

import titan.core.bluetooth.CommManager
import titan.core.products.*
import titan.core.toDecimal
import java.util.*
import kotlin.collections.ArrayList

/*
0329AA090F1200
Position0: Command_Id
Position1: Key_Id
Position2: switch(AA) -> open
Position3: start_hour(09) -> 24H format
Position4: start_minute(0F) -> 15
position5: end_hour(12) -> 18 -> 24H format
position6: end_minute(00)
position7: have_time_range Is there a time range New field, function table, 0x00 invalid, 0x01 means no time range, 0x02 means time range
position8: week_repeat Repeat week, same as sedentary and alarm clock, bit0 invalid, (bit1-bit7) for week Monday to Sunday
Remaining 13 bytes are reserved
* */

private const val commandId: Byte = 3
private const val keyId: Byte = 41

class R3SetDNDCommand : DataCommand {
    private var listener: DataCallback<CoreDND>? = null

    fun set(dnd: CoreDND): R3SetDNDCommand {
        val data: ByteArray = byteArrayOf(
            commandId,
            keyId,
            if (dnd.isOn) -86 else 85,
            dnd.start.first.toByte(),
            dnd.start.second.toByte(),
            dnd.end.first.toByte(),
            dnd.end.second.toByte(),
            if (dnd.isTimeBased) 2 else 1,
            repetitionDays(
                dnd.repeatWeekly,
                dnd.repeatDaysList
            )
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

    private fun repetitionDays(isRepeat: Boolean, repetitions: ArrayList<DaysList>): Byte {
        var repeatDaysString = ""
        repeatDaysString = if (isRepeat) {
            repeatDaysString.plus("1")
        } else {
            repeatDaysString.plus("0")
        }
        DaysList.getDaysList(weekDay = DaysList.MONDAY).forEach {
            repeatDaysString = if (repetitions.contains(it)) {
                repeatDaysString.plus("1")
            } else {
                repeatDaysString.plus("0")
            }
        }
        return repeatDaysString.reversed().toDecimal().toByte()
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

    fun callback(listener: DataCallback<CoreDND>): R3SetDNDCommand {
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