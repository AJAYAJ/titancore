package titan.core.products.reflex_2

import android.content.Context
import android.text.format.DateFormat
import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import titan.core.toInt
import java.util.*

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */


private const val command: Byte = -66
private const val id: Byte = 1
private const val key: Byte = 1
private const val endKey: Byte = -34

internal class R2BaseTimeCommand constructor(private val context: Context) : DataCommand {
    private var listener: DataCallback<DeviceTime>? = null

    fun get(): R2BaseTimeCommand {
        val data = byteArrayOf(
            command,
            id,
            key,
            -19
        )
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

    fun set(baseTime: DeviceTime.BaseTime): R2BaseTimeCommand {
        val is24 = (if (DateFormat.is24HourFormat(context)) 0 else 1).toByte()

        val year = baseTime.calendar[Calendar.YEAR]
        val month = baseTime.calendar[Calendar.MONTH] + 1
        val day = baseTime.calendar[Calendar.DAY_OF_MONTH]
        val week = baseTime.calendar[Calendar.DAY_OF_WEEK] - 1
        val hour = baseTime.calendar[Calendar.HOUR_OF_DAY]
        val minute = baseTime.calendar[Calendar.MINUTE]
        val seconds = baseTime.calendar[Calendar.SECOND]

        val data = byteArrayOf(
            command,
            id,
            key,
            -2,
            if (baseTime.isMetric) 0 else 1,
            is24,
            baseTime.activeTimeZone.toByte(),
            baseTime.currentTimeZone.toByte(),
            (year shr 8).toByte(),
            year.toByte(),
            month.toByte(),
            day.toByte(),
            week.toByte(),
            hour.toByte(),
            minute.toByte(),
            seconds.toByte()
        )
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

    fun callback(listener: DataCallback<DeviceTime>): R2BaseTimeCommand {
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
        println(byteArray[3].asInt())
        when (byteArray[3].asInt()) {
            251 -> {
                if (byteArray.size<16){
                    listener?.onResult(Response.Status(false))
                    return
                }
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, byteArrayOf(byteArray[8], byteArray[9]).toInt(false))
                    set(Calendar.MONTH, byteArray[10].toInt() - 1)
                    set(Calendar.DAY_OF_MONTH, byteArray[11].toInt())
                    set(Calendar.HOUR_OF_DAY, byteArray[13].toInt())
                    set(Calendar.MINUTE, byteArray[14].toInt())
                    set(Calendar.SECOND, byteArray[15].toInt())
                }
                val isMetric = byteArray[4].toInt()
                listener?.onResult(
                    Response.Result(
                        DeviceTime.BaseTime(
                            calendar = calendar,
                            isMetric = isMetric == 0,
                            activeTimeZone = byteArray[6].toInt(),
                            currentTimeZone = byteArray[7].toInt()
                        )
                    )
                )
            }
            237 -> {
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