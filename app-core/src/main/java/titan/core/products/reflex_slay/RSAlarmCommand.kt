package titan.core.products.reflex_slay

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.convertToBinaryString
import titan.core.products.*
import titan.core.toDecimal
import java.util.*

private const val messageId: Byte = 53
private const val messageLength: Byte = 18
private const val endKey: Byte = 2
private const val endId: Byte = -1

internal class RSAlarmCommand : DataCommand {
    private var listener: DataCallback<CoreAlarmSet>? = null

    fun getAlarmData(): RSAlarmCommand {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                read = null,
                write = Pair(
                    RS_COMMUNICATION_SERVICE,
                    RS_COMMUNICATION_WRITE_CHAR
                ),
                responseWillNotify = true,
                data = byteArrayOf(1, 7, 0),
                key = getKey()
            )
        }
        return this
    }

    fun setAlarmData(alarmSet: CoreAlarmSet): RSAlarmCommand {
        val alarm: CoreAlarm = alarmSet.list[0]
        val data: ByteArray = byteArrayOf(
            messageLength,
            messageId,
            alarm.alarmId.toByte(),
            if(alarm.enableAlarm) 1.toByte() else 0.toByte(),
            repetitionDays(
                alarm.repeatAlarmWeekly && alarm.enableAlarm,
                alarm.alarmRepetitionDays
            ),
            alarm.hour.toByte(),
            alarm.minute.toByte(),
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0
        )
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    RS_COMMUNICATION_SERVICE,
                    RS_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = data,
                key = getKey()
            )
        }
        return this
    }

    private fun repetitionDays(repeatAlarm: Boolean, repetitionDays: ArrayList<DaysList>): Byte {
        var repeatDaysString = ""
        enumValues<DaysList>().forEach {
            repeatDaysString = if (repetitionDays.contains(it)) {
                repeatDaysString.plus("1")
            } else {
                repeatDaysString.plus("0")
            }
        }
        repeatDaysString = if (repeatAlarm) {
            repeatDaysString.plus("1")
        } else {
            repeatDaysString.plus("0")
        }
        return repeatDaysString.reversed().toDecimal().toByte()
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != endId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 4) {
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    private fun parse(data: ByteArray) {
        when (data[2].asInt()) {
            53 -> {
                listener?.onResult(Response.Status(true))
            }
            7 -> {
                /*Get Method Req Response will handle here*/

            }
        }
    }

    fun callback(listener: DataCallback<CoreAlarmSet>): RSAlarmCommand {
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