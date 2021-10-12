package titan.core.products.reflex_2

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/*
* The mobile phone sends the event information to the device (sedentary reminder)
Data: 19Bytes BE+01+0C+FE+enable switch (1byte) +
starting hour 1 (1byte) + starting minute 1 (1byte) + ending hour 1 (1byte) + ending minute 1 (1byte)
starting hour 2 (1byte) + starting minute 2 (1byte) + ending hour 2 (1byte) + ending minute 2 (1byte)
starting hour 3 (1byte) + starting minute 3 (1byte) + ending hour 3 (1byte) + ending minute 3 (1byte)
+ rest hour(1byte)+ rest min(1byte)
Enable switch (0 for disable, 1 for enable)
*/

private const val command: Byte = -66
private const val id: Byte = 1
private const val key: Byte = 12
private const val endKey: Byte = -34

internal class R2SedentaryReminderCommand : DataCommand {
    private var listener: DataCallback<SedentaryReminder>? = null

    fun setData(sedentaryReminder: SedentaryReminder): R2SedentaryReminderCommand {
        var index = 0
        val data = ByteArray(19)
        data[index++] = command
        data[index++] = id
        data[index++] = key
        data[index++] = -2
        data[index++] = 0
        for (i in 1..3) {/* 3 is Max for sedentary list*/
            if (sedentaryReminder.list.size >= i) {
                val item = sedentaryReminder.list[i - 1]
                data[index++] = item.start.first.toByte()
                data[index++] = item.start.second.toByte()
                data[index++] = item.end.first.toByte()
                data[index++] = item.end.second.toByte()
            } else {
                data[index++] = -2
                data[index++] = -2
                data[index++] = -2
                data[index++] = -2
            }
        }
        data[4] = if (sedentaryReminder.isSedentaryOn) 1 else 0
        data[index++] = (sedentaryReminder.restTime / 60).toByte()
        data[index] = (sedentaryReminder.restTime % 60).toByte()
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

    fun getData(): R2SedentaryReminderCommand {
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

    fun callback(listener: DataCallback<SedentaryReminder>): R2SedentaryReminderCommand {
        this.listener = listener
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && (byteArray[1] != id || byteArray[2] != key)) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 3 && !(byteArray[3].asInt() == 254 || byteArray[3].asInt() == 237)) {
            ResponseStatus.INCOMPATIBLE
        } else if ((byteArray[3].asInt() == 254 && byteArray.size < 19) || (byteArray[3].asInt() == 237 && byteArray.size < 4) ) {
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
            254 -> {
                val data: ArrayList<Sedentary> = arrayListOf()
                if (byteArray.size >= 19) {
                    if ((byteArray[5].asInt() != -2 && byteArray[6].asInt() != -2 && byteArray[7].asInt() != -2 && byteArray[8].asInt() != -2) || (byteArray[5].asInt() != 0 && byteArray[6].asInt() != 0 && byteArray[7].asInt() != 0 && byteArray[8].asInt() != 0)) {
                        data.add(
                            Sedentary(
                                start = Pair(byteArray[5].asInt(), byteArray[6].asInt()),
                                end = Pair(byteArray[7].asInt(), byteArray[8].asInt()),
                                interval = 30,
                                isSedentaryOn = byteArray[4].asInt() == 1
                            )
                        )
                    }
                    val reminder = SedentaryReminder(
                        list = data,
                        restTime = (byteArray[17].asInt()) * 60 + (byteArray[18].asInt()),
                        isSedentaryOn = byteArray[4].asInt() == 1 && !data.isNullOrEmpty()
                    )
                    listener?.onResult(Response.Result(reminder))
                }
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