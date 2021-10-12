package titan.core.products.reflex_2

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*
import kotlin.collections.HashMap

/*
* *COMMAND: BE-01-19-FE-ON/OFF(00: OFF 01: ON) + Time (5 minutes - 60 minutes)
* recurringPeriodInMin means read heart rate for every [x] min
* recurringPeriodInMin will start from 5 to 60 min and it is multiple's of 5 only.
* if auto heart rate detection is off there is no recurringPeriodInMin*/

private const val command: Byte = -66
private const val id: Byte = 1
private const val key: Byte = 25
private const val endKey: Byte = -34

internal class R2AutoHeartRateCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null
    private var parseData = HashMap<Int, ByteArray>()

    fun set(
        recurringPeriodInMin: Int,
        detectHRAutomatically: Boolean
    ): R2AutoHeartRateCommand {
        val data = byteArrayOf(command, id, key, -2, if (detectHRAutomatically) 1 else 0, recurringPeriodInMin.toByte())
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

    fun callback(callback: DataCallback<Boolean>): R2AutoHeartRateCommand {
        listener = callback
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
            parseData[0] = byteArray
            parse(parseData)
            parseData.clear()
            ResponseStatus.COMPLETED
        }
    }

    fun parse(data: HashMap<Int, ByteArray>) {
        if (data.isNotEmpty() && data.containsKey(0)) {
            data[0]?.let { byteArray ->
                when (byteArray[3].asInt()) {
                    237 -> {
                        listener?.onResult(Response.Status(true))
                    }
                    else -> {
                        listener?.onResult(Response.Status(false))
                    }
                }
            }
        } else {
            listener?.onResult(Response.Status(false))
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