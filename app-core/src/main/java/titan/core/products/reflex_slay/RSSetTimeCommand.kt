package titan.core.products.reflex_slay

import titan.core.bluetooth.CommManager
import titan.core.getLocalCalendar
import titan.core.products.*
import titan.core.toBytes
import java.util.*

/*
07-30-E4-07-09-03-13-28-10
Index: 0, Size: 1, Additional Info: Length of message body
Index: 1, Size: 1, Additional Info: Message Id
Index: 2, Size: 2, Type: Uint16, Range: 1900-2020, Additional Info: Year ->(07E4)
Index: 4, Size: 1, Type: Uint8, Range: 1-12, Additional Info: Month ->(09)
Index: 5, Size: 1, Type: Uint8, Range: 1-31, Additional Info: Day ->(03)
Index: 6, Size: 1, Type: Uint8, Range: 0-23, Additional Info: Hour ->(13 -> 19)
Index: 7, Size: 1, Type: Uint8, Range: 0-59, Additional Info: Minute ->(28 -> 40)
Index: 8, Size: 1, Type: Uint8, Range: 0-59, Additional Info: Seconds ->(10 -> 16)*/

fun main(){
    val model: RSSetTimeCommand = RSSetTimeCommand()
     model.set()
    val calendar = getLocalCalendar()
    val data = byteArrayOf(
        messageLength,
        messageId,
        calendar.get(Calendar.YEAR).toBytes()[1],
        calendar.get(Calendar.YEAR).toBytes()[0],
        (calendar.get(Calendar.MONTH) + 1).toByte(),
        calendar.get(Calendar.DATE).toByte(),
        calendar.get(Calendar.HOUR_OF_DAY).toByte(),
        calendar.get(Calendar.MINUTE).toByte(),
        calendar.get(Calendar.SECOND).toByte()
    )

    println(Arrays.toString(data))

}

private const val messageId: Byte = 48
private const val messageLength: Byte = 7
private const val endKey: Byte = 2
private const val endId: Byte = -1
internal class RSSetTimeCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null

    fun set(): RSSetTimeCommand {
        val calendar = getLocalCalendar()
        val year = calendar[Calendar.YEAR]
        val month = calendar[Calendar.MONTH] + 1
        val day = calendar[Calendar.DAY_OF_MONTH]
        val hour = calendar[Calendar.HOUR_OF_DAY]
        val minute = calendar[Calendar.MINUTE]
        val seconds = calendar[Calendar.SECOND]
        val data = byteArrayOf(
            messageLength,
            messageId,
            year.toByte(),
            (year shr 8).toByte(),
            month.toByte(),
            day.toByte(),
            hour.toByte(),
            minute.toByte(),
            seconds.toByte()
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
        println(data)
        return this
    }

    fun callback(listener: DataCallback<Boolean>): RSSetTimeCommand {
        this.listener = listener
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != endId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 4) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            listener?.onResult(Response.Status(true))
            ResponseStatus.COMPLETED
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