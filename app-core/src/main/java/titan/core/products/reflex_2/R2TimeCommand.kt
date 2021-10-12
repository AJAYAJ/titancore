package titan.core.products.reflex_2

import android.content.Context
import android.text.format.DateFormat
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*
import kotlin.math.abs

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */
/*
* Command : Data: 14Bytes BE+01+02+FE+ calendar year (2Byte) + calendar month (1Byte) + Calendar day (1Byte) + week (1Byte) + time zone (1Byte) + hour (1Byte) + minute (1Byte) + second (1Byte) +12H/24H (1Byte)
* */

private const val command: Byte = -66
private const val id: Byte = 1
private const val key: Byte = 2
private const val endKey: Byte = -34

internal class R2TimeCommand constructor(private val context: Context) : DataCommand {
    private var listener: DataCallback<Boolean>? = null

    fun set(): R2TimeCommand {
        val offset = TimeZone.getDefault().rawOffset / 3600000
        val currentTimeZone =
            (if (offset < 0) abs(offset) * 2 + 0x80 else abs(
                offset
            ) * 2).toByte()
        val calendar = Calendar.getInstance()
        val year = calendar[Calendar.YEAR]
        val month = calendar[Calendar.MONTH] + 1
        val day = calendar[Calendar.DAY_OF_MONTH]
        val week = calendar[Calendar.DAY_OF_WEEK] - 1
        val hour = calendar[Calendar.HOUR_OF_DAY]
        val minute = calendar[Calendar.MINUTE]
        val seconds = calendar[Calendar.SECOND]
        val is24 = (if (DateFormat.is24HourFormat(context)) 0 else 1).toByte()
        val data = byteArrayOf(
            command,
            id,
            key,
            -2,
            (year shr 8).toByte(),
            year.toByte(),
            month.toByte(),
            day.toByte(),
            week.toByte(),
            currentTimeZone,
            hour.toByte(),
            minute.toByte(),
            seconds.toByte(),
            is24
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

    fun callback(listener: DataCallback<Boolean>): R2TimeCommand {
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
            listener?.onResult(Response.Status(true))
            ResponseStatus.COMPLETED
        }
    }

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }
}