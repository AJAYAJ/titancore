package titan.core.products.reflex_3

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import titan.core.toDecimal
import java.util.*
import kotlin.collections.ArrayList

/*
To Send data:
03020155070F2D0501
Position0: Command_Id
Position1: Key_Id
Position2: Alarm Id, Should be in incremental order(1-max count)
Position3: Show_Status (55) -> show
Position4: Type (07) -> others
Position5: Hour
Position6: Minute
Position7: Repetitions (00000101)
Position8: Snooze_duration ( 0 for no snooze , or min)
Snooze not working
Remaining 11 bytes are reserved
* */

private const val commandId: Byte = 3
private const val keyId: Byte = 2

class R3SetAlarmCommand : DataCommand {
    private var listener: DataCallback<CoreAlarmSet>? = null
    fun setData(coreAlarmSet: CoreAlarmSet): R3SetAlarmCommand {
        if (!coreAlarmSet.list.isNullOrEmpty()) {
            val coreAlarm: CoreAlarm = coreAlarmSet.list[0]
            val data: ByteArray = byteArrayOf(
                commandId,
                keyId,
                coreAlarm.alarmId.toByte(),
                if (coreAlarm.isDelete) 170.toByte() else 85.toByte(),
                AlarmType.getReflex3Conversion(coreAlarm.alarmType),
                coreAlarm.hour.toByte(),
                coreAlarm.minute.toByte(),
                repetitionDays(
                    coreAlarm.repeatAlarmWeekly && coreAlarm.enableAlarm,
                    coreAlarm.alarmRepetitionDays
                ),
                coreAlarm.snoozeDuration.toByte()
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
        } else {
            listener?.onResult(Response.Status(false))
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
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun callback(listener: DataCallback<CoreAlarmSet>): R3SetAlarmCommand {
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
        /*Status：（0x00:successful ，0x01：failed-Exceeds the maximum
        0x02：failed-id Already occupied， 0x03：failed-id invalid）*/
        if (data.size > 3) {
            when (data[2].asInt()) {
                0 -> {
                    listener?.onResult(Response.Status(true))
                }
                1 -> {
                    listener?.onResult(Response.Status(false))
                }
                2 -> {

                }
            }
        } else {
            listener?.onResult(Response.Status(true))
        }
    }
}