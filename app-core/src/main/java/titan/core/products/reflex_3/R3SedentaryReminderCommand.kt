package titan.core.products.reflex_3

import titan.core.bluetooth.CommManager
import titan.core.products.*
import titan.core.toBytes
import titan.core.toDecimal
import java.util.*
import kotlin.collections.ArrayList


/*0320090F120010E7
    Position0: Command_Id
    Position1: Key_Id
    Position2: start_hour(09) -> 24H format
    Position3 : start_minute(0F) ->15
    Position4: end_hour(12) -> 18 -> 24H format
    Position5: end_minute(00)
    Position6: interval(10 00)-> 16 min's(as value to be bigger than 15 min's)
    Interval value length is 2 bytes
    Position7: repetitions(E7) -> 11100111
    Remaining 11 bytes are reserved*/

private const val commandId: Byte = 3
private const val keyId: Byte = 32

class R3SedentaryReminderCommand : DataCommand {
    private var listener: DataCallback<SedentaryReminder>? = null
    fun set(sedentaryReminder: SedentaryReminder): R3SedentaryReminderCommand {
        val repetition: ByteArray = repetitionDays(
            sedentaryReminder.list[0].repeatSedentary,
            sedentaryReminder.list[0].repeatDaysList
        )
        val sedentary: Sedentary = sedentaryReminder.list[0]
        val data: ByteArray = byteArrayOf(
            commandId,
            keyId,
            sedentary.start.first.toByte(),
            sedentary.start.second.toByte(),
            sedentary.end.first.toByte(),
            sedentary.end.second.toByte(),
            sedentary.interval.toBytes()[1],
            sedentary.interval.toBytes()[0],
            repetition[1],
            repetition[0]
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

    private fun repetitionDays(isRepeat: Boolean, repetitions: ArrayList<DaysList>): ByteArray {
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
        return repeatDaysString.reversed().toDecimal().toBytes()
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

    fun callback(listener: DataCallback<SedentaryReminder>): R3SedentaryReminderCommand {
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