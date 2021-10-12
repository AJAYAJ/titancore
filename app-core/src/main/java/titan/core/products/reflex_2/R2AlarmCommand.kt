package titan.core.products.reflex_2

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.convertToBinaryString
import titan.core.products.*
import titan.core.toDecimal
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

/*To send data:
BE-01-09-FE-05-11-0A-2D-11-0F-52-11-14-15-11-19-1D-11-1E-31
Position4: alarm clock switch(set 5 groups) -> 00101 -> 05
Position5: first alarm hour
Position6: first alarm minutes
Position7: alarm1 repeated logo -> 00101101 -> 2D
Position8: second alarm hour
Position9: second alarm minute
Position10: alarm2 repeated logo -> 01010010 ->52
Position11: third alarm hour
Position12: third alarm minute
Position13: alarm3 repeated logo -> 00010101 -> 15
Position14: fourth alarm hour
Position15: fourth alarm minute
Position16: alarm4 repeated logo -> 00011101 -> 1D
Position17: fifth alarm hour
Position18: fifth alarm minute
Position19: alarm5 repeated logo -> 00110001 -> 31
No display for alarm
* */

private const val command: Byte = -66
private const val id: Byte = 1
private const val key: Byte = 9
private const val endKey: Byte = -34
const val R2_ALARM = "r2_alarm_set"

internal class R2AlarmCommand : DataCommand {
    private var listener: DataCallback<CoreAlarmSet>? = null
    fun getAlarmData(): R2AlarmCommand {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                read = null,
                write = Pair(
                    R2_COMMUNICATION_SERVICE,
                    R2_COMMUNICATION_WRITE_CHAR
                ),
                responseWillNotify = true,
                data = byteArrayOf(command, id, key, -19),
                key = getKey()
            )
        }
        return this
    }

    fun setAlarmData(req: CoreAlarmSet): R2AlarmCommand {
        var index = 0
        val data = ByteArray(20)
        var alarmSwitch = ""/* Will tell whether alarm is enabled or not.*/
        data[index++] = command
        data[index++] = id
        data[index++] = key
        data[index++] = -2
        data[index++] = 0
        for (i in 1..5) {
            if (req.list.size >= i) {
                val item = req.list[i - 1]
                alarmSwitch = alarmSwitch.plus(if (item.enableAlarm) "1" else "0")
                data[index++] = item.hour.toByte()
                data[index++] = item.minute.toByte()
                data[index++] = getRepeatedDays(item.repeatAlarmWeekly, item.alarmRepetitionDays)
            } else {
                alarmSwitch = alarmSwitch.plus("0")
                data[index++] = -2
                data[index++] = -2
                data[index++] = getRepeatedDays(false, arrayListOf())
            }
        }
        data[4] = alarmSwitch.reversed().toDecimal().toByte()
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R2_COMMUNICATION_SERVICE,
                    R2_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = data,
                key = getKey()
            )
        }
        return this
    }

    private fun getRepeatedDays(repeatAlarm: Boolean, repetitionDays: ArrayList<DaysList>): Byte {
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
        } else if (byteArray.size > 2 && (byteArray[1] != id || byteArray[2] != key)) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 4) {
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun callback(listener: DataCallback<CoreAlarmSet>): R2AlarmCommand {
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

    private fun parse(data: ByteArray) {
        when (data[3].asInt()) {
            237 -> {
                listener?.onResult(Response.Status(true))
            }
            254 -> {
                /*Get Method Req Response will handle here*/
                val list: ArrayList<CoreAlarm> = ArrayList()
                var id = 1
                if (data.size == 20) {
                    val groupSwitch = data[4]
                    val isAlarmSetPreviously = data.contains(-1)
                    for (i in 5..19 step 3) {
                        val isValid = data[i].asInt() != 0 || data[i + 1].asInt() != 0
                        if (isValid) {/*If all zero means empty record*/
                            val repeatDaysSwitch = data[i + 2].convertToBinaryString().reversed()
                            val repetitionDays: ArrayList<DaysList> = ArrayList()
                            for (day in 0..6) {
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
                            list.add(
                                CoreAlarm(
                                    alarmId = id,
                                    hour = data[i].toInt(),
                                    minute = data[i + 1].toInt(),
                                    description = "",
                                    enableAlarm = groupSwitch.convertToBinaryString()
                                        .reversed()[id - 1].toString() == "1",
                                    repeatAlarmWeekly = repeatDaysSwitch[7].toString() == "1",
                                    alarmRepetitionDays = repetitionDays
                                )
                            )
                            id++
                        }
                    }
                    listener?.onResult(Response.Result(CoreAlarmSet(list)))
                }
            }
        }
    }
}