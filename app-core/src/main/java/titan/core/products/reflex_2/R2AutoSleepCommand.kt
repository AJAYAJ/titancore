package titan.core.products.reflex_2

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.getLocalCalendar
import titan.core.products.*
import java.util.*

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

private const val command: Byte = -66
private const val id: Byte = 1
private const val key: Byte = 7
private const val endKey: Byte = -34

internal class R2AutoSleepCommand : DataCommand {
    private var listener: DataCallback<AutoSleep>? = null

    fun set(autoSleep: AutoSleep): R2AutoSleepCommand {
        var sleepReminderHour: Int = -2
        var sleepReminderMin: Int = -2
        if (autoSleep.remindSleep && autoSleep.sleepReminderMinutes>0){
            val sleepStartCalendar = getLocalCalendar().apply {
                set(Calendar.HOUR_OF_DAY,autoSleep.sleepStart.first)
                set(Calendar.MINUTE,autoSleep.sleepStart.second)
            }
            val reminderCalendar = sleepStartCalendar.apply {
                set(Calendar.MINUTE,-autoSleep.sleepReminderMinutes)
            }
            sleepReminderHour = reminderCalendar.get(Calendar.HOUR_OF_DAY)
            sleepReminderMin = reminderCalendar.get(Calendar.MINUTE)
        }
        val napReminderHour: Int = (autoSleep.napReminderMinutes) / 60
        val napReminderMin: Int = (autoSleep.napReminderMinutes) % 60

        val data: ByteArray = byteArrayOf(
            command,
            id,
            key,
            -2,
            if (autoSleep.autoSleep) 1.toByte() else 0.toByte(),
            autoSleep.sleepStart.first.toByte(),
            autoSleep.sleepStart.second.toByte(),
            if (!autoSleep.remindSleep || sleepReminderHour == 0) -2 else sleepReminderHour.toByte(),
            if (!autoSleep.remindSleep || sleepReminderMin == 0) -2 else sleepReminderMin.toByte(),
            autoSleep.sleepEnd.first.toByte(),
            autoSleep.sleepEnd.second.toByte(),
            if (autoSleep.remindNap || autoSleep.napStart.first != 0) autoSleep.napStart.first.toByte() else -2,
            if (autoSleep.remindNap || autoSleep.napStart.second != 0) autoSleep.napStart.second.toByte() else -2,
            if (autoSleep.remindNap || autoSleep.napEnd.first != 0) autoSleep.napEnd.first.toByte() else -2,
            if (autoSleep.remindNap || autoSleep.napEnd.second != 0) autoSleep.napEnd.second.toByte() else -2,
            if (autoSleep.remindNap || napReminderHour != 0) napReminderHour.toByte() else -2,
            if (autoSleep.remindNap || napReminderMin != 0) napReminderMin.toByte() else -2
        )
        if (!autoSleep.autoSleep)
            data.fill(-2, 5, 16)

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

    fun getData(): R2AutoSleepCommand {
        val data = byteArrayOf(command, id, key, -19)
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

    fun callback(listener: DataCallback<AutoSleep>): R2AutoSleepCommand {
        this.listener = listener
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && (byteArray[1] != id || byteArray[2] != key)) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 4) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun parse(byteArray: ByteArray) {
        when (byteArray[3].asInt()) {
            237 -> {
                listener?.onResult(Response.Status(true))
            }
            254 -> {
                if (byteArray.size == 17) {
                    val sleepStartHour = if (byteArray[5].asInt() == 254) 0 else byteArray[5].asInt()
                    val sleepStartMin = if (byteArray[6].asInt() == 254) 0 else byteArray[6].asInt()
                    val sleepEndHour = if (byteArray[9].asInt() == 254) 0 else byteArray[9].asInt()
                    val sleepEndMin = if (byteArray[10].asInt() == 254) 0 else byteArray[10].asInt()
                    val reminderHour = if (byteArray[7].asInt() == 254) 0 else byteArray[7].asInt()
                    val reminderMin = if (byteArray[8].asInt() == 254) 0 else byteArray[8].asInt()
                    val remindSleepBeforeInMIN = (reminderHour * 60 + reminderMin)
                    /*Assuming as alarm didn't set before from this application or it is disabled*/
                    if (byteArray[4].asInt()==0 && byteArray[5].asInt() == 254 && byteArray[6].asInt() == 254 && byteArray[9].asInt() == 254 && byteArray[10].asInt() == 254){
                        listener?.onResult(Response.Status(true))
                        return
                    }
                    val autoSleep = AutoSleep(
                        autoSleep = byteArray[4].asInt() == 1,
                        sleepStart = Pair(sleepStartHour, sleepStartMin),
                        sleepReminderMinutes = remindSleepBeforeInMIN,
                        sleepEnd = Pair(sleepEndHour, sleepEndMin),
                        napStart = Pair(0, 0),
                        napEnd = Pair(0, 0),
                        napReminderMinutes = 0,
                        remindSleep = remindSleepBeforeInMIN > 0,
                        remindNap = false
                    )
                    listener?.onResult(Response.Result(autoSleep))
                } else {
                    listener?.onResult(Response.Status(true))
                }
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