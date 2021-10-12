package titan.core.products.reflex_2

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import titan.core.toDate
import titan.core.toInt
import java.util.*

private const val command: Byte = -66
private const val id: Byte = 2
private const val key: Byte = 18
private const val endKey: Byte = -34

/*
* COMMAND: BE-02-12-ED
* REPLY: DE-02-12-FB-Starting Year(2Bytes)-Starting Month(1Bytes)-Starting Day(Bytes)-Ending Year(2Bytes)-Ending Month(1Bytes)-Ending Day(1Bytes)
* if there is no data starting month, year ,day will get as 00
* */
internal class R2HistoricalHeartRateDatesCommand : DataCommand {
    private var listener: DataCallback<CoreHistoricalDates>? = null

    fun get(): R2HistoricalHeartRateDatesCommand {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R2_COMMUNICATION_SERVICE,
                    R2_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(command, id, key, -19),
                key = getKey()
            )
        }
        return this
    }

    fun check(packet: ByteArray): ResponseStatus {
        return if (packet.isNotEmpty() && packet[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (packet.size > 2 && (packet[1] != id || packet[2] != key)) {
            ResponseStatus.INCOMPATIBLE
        } else if (packet.size < 12) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(packet)
            ResponseStatus.COMPLETED
        }
    }

    fun callback(callback: DataCallback<CoreHistoricalDates>): R2HistoricalHeartRateDatesCommand {
        listener = callback
        return this
    }

    fun parse(packet: ByteArray) {
        when (packet[3].asInt()) {
            251 -> {
                if (byteArrayOf(packet[5], packet[4]).toInt() == 0
                    || byteArrayOf(packet[9], packet[8]).toInt() == 0
                    || packet[6].asInt() == 0
                    || packet[7].asInt() == 0
                    || packet[10].asInt() == 0
                    || packet[11].asInt() == 0
                ) {
                    listener?.onResult(Response.Status(true))
                } else {
                    listener?.onResult(
                        Response.Result(
                            CoreHistoricalDates(
                                startDate = Triple(
                                    byteArrayOf(packet[5], packet[4]).toInt(),
                                    packet[6].asInt(),
                                    packet[7].asInt()
                                ).toDate(),
                                endDate = Triple(
                                    byteArrayOf(packet[9], packet[8]).toInt(),
                                    packet[10].asInt(),
                                    packet[11].asInt()
                                ).toDate()
                            )
                        )
                    )
                }
            }
            else -> {
                listener?.onResult(Response.Status(false, "Un expected error"))
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