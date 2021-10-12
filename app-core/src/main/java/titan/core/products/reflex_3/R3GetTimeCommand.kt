package titan.core.products.reflex_3

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.getLocalCalendar
import titan.core.products.*
import titan.core.toInt
import java.util.*

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

/** To Send data:
030107E402190C223201
Position0: Command_Id
Position1: Key_Id
Position2 & 3: Year (2 bytes) -> lower order in front
Position4: Month
Position5: Day
Position6: Hour
Position7: Minute
Position8: Second
Position9: Week_day
Remaining 10 bytes are reserved
 */

private const val commandId: Byte = 2
private const val keyId: Byte = 3

internal class R3GetTimeCommand : DataCommand {
    private var listener: DataCallback<DeviceTime>? = null

    fun get(): R3GetTimeCommand {
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

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }

    private fun getDayOfWeek(day: Int): DaysList {
        return when (day) {
            0 -> DaysList.MONDAY
            1 -> DaysList.TUESDAY
            2 -> DaysList.WEDNESDAY
            3 -> DaysList.THURSDAY
            4 -> DaysList.FRIDAY
            5 -> DaysList.SATURDAY
            6 -> DaysList.SUNDAY
            else -> DaysList.MONDAY
        }
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != commandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 10) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    private fun parse(data: ByteArray) {
        val calendar = getLocalCalendar().apply {
            set(Calendar.YEAR, byteArrayOf(data[2], data[3]).toInt())
            set(Calendar.MONTH, data[4].toInt() - 1)
            set(Calendar.DAY_OF_MONTH, data[5].toInt())
            set(Calendar.HOUR_OF_DAY, data[6].toInt())
            set(Calendar.MINUTE, data[7].toInt())
            set(Calendar.SECOND, data[8].toInt())
            set(Calendar.SECOND, data[8].toInt())
        }
        listener?.onResult(
            Response.Result(
                DeviceTime.R3DeviceTime(
                    calendar = calendar,
                    weekDay = getDayOfWeek(data[9].asInt())
                )
            )
        )
    }

    fun callback(listener: DataCallback<DeviceTime>): R3GetTimeCommand {
        this.listener = listener
        return this
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }
}