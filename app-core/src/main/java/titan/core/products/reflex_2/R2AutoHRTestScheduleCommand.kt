package titan.core.products.reflex_2

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

private const val command: Byte = -66
private const val id: Byte = 1
private const val key: Byte = 15
private const val endKey: Byte = -34

internal class R2AutoHRTestScheduleCommand : DataCommand {
    private var listener: DataCallback<AutoHRSchedule>? = null

    fun setData(autoHRSchedule: AutoHRSchedule): R2AutoHRTestScheduleCommand {
        autoHRSchedule.isHREnable = true
        autoHRSchedule.isHREnabledMorning = true
        autoHRSchedule.isHREnabledAfterNoon = true
        autoHRSchedule.isHREnabledNight = false
        val data = ByteArray(17)
        data.plus(byteArrayOf(command, id, key, -2))
        if (autoHRSchedule.isHREnable) {
            data.plus(
                byteArrayOf(
                    if (autoHRSchedule.isHREnable) 1 else 0,
                    autoHRSchedule.morningStart.first.convertToByte(autoHRSchedule.isHREnabledMorning),
                    autoHRSchedule.morningStart.second.convertToByte(autoHRSchedule.isHREnabledMorning),
                    autoHRSchedule.morningEnd.first.convertToByte(autoHRSchedule.isHREnabledMorning),
                    autoHRSchedule.morningEnd.second.convertToByte(autoHRSchedule.isHREnabledMorning),
                    autoHRSchedule.afterNoonStart.first.convertToByte(autoHRSchedule.isHREnabledAfterNoon),
                    autoHRSchedule.afterNoonStart.second.convertToByte(autoHRSchedule.isHREnabledAfterNoon),
                    autoHRSchedule.afterNoonEnd.first.convertToByte(autoHRSchedule.isHREnabledAfterNoon),
                    autoHRSchedule.afterNoonEnd.second.convertToByte(autoHRSchedule.isHREnabledAfterNoon),
                    autoHRSchedule.nightStart.first.convertToByte(autoHRSchedule.isHREnabledNight),
                    autoHRSchedule.nightStart.second.convertToByte(autoHRSchedule.isHREnabledNight),
                    autoHRSchedule.nightEnd.first.convertToByte(autoHRSchedule.isHREnabledNight),
                    autoHRSchedule.nightEnd.second.convertToByte(autoHRSchedule.isHREnabledNight)
                )
            )
        } else {
            data.fill(-2, 5, 16)
        }
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

    private fun Int.convertToByte(value: Boolean): Byte {
        return if (value) this.toByte() else -2
    }

    fun callback(listener: DataCallback<AutoHRSchedule>): R2AutoHRTestScheduleCommand {
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
                /*Set Method Req Response will handle here*/
                listener?.onResult(Response.Status(true))
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