package titan.core.products.reflex_2

import titan.core.*
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*
import kotlin.collections.HashMap

/**
 * COMMAND:BE-02-10-FE-YY(Year)(2byte)-MM(Month)(1bytes)-DD(Day)(1bytes)
 * -00(Means start from which data packet. The default from app is 00)
 * In Key1 first 10 positions fill with default values.
 * Observations: Getting more values greater than total count,
 * so we need to take values as per the total count. Need to remove excess values.
 * */

private const val command: Byte = -66
private const val id: Byte = 2
private const val key: Byte = 16
private const val endKey: Byte = -34

internal class R2HeartRateCommand : DataCommand {
    private var listener: DataCallback< CoreHeartRate>? = null
    private var incomingPackets = HashMap<Int, ByteArray>()
    private var packetSequence = 0

    fun get(date: Date): R2HeartRateCommand {
        val data = byteArrayOf(
            command,
            id,
            key,
            -2,
            (date.toYear() shr 8).toByte(),
            date.toYear().toByte(),
            date.toMonth().toByte(),
            date.toDay().toByte(),
            0
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

    fun callback(callback: DataCallback<CoreHeartRate>): R2HeartRateCommand {
        listener = callback
        return this
    }

    fun check(packet: ByteArray): ResponseStatus {
        return if (packetSequence > 0) {
            if (packet.size < 4) {
                listener?.onResult(Response.Status(false))
                ResponseStatus.INVALID_DATA_LENGTH
            } else if (packet[0] == endKey && packet[1] == id && packet[2] == key
                && packet[3] == (-19).toByte()
            ) {
                var dataLength = 0
                incomingPackets.forEach {
                    if (it.key > 1) {
                        dataLength += it.value.size
                    }
                }
                val data = parse()
                if (data != null) {
                    listener?.onResult(Response.Result(data))
                } else {
                    listener?.onResult(Response.Status(false))
                }
                ResponseStatus.COMPLETED
            } else {
                incomingPackets[++packetSequence] = packet
                return ResponseStatus.INCOMPLETE
            }
        } else if (packet.isNotEmpty() && packet[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (packet.size > 2 && (packet[1] != id || packet[2] != key)) {
            ResponseStatus.INCOMPATIBLE
        } else if (packet.size > 3 && packet[3].asInt() == 6) {
            listener?.onResult(Response.Status(true))
            ResponseStatus.COMPLETED
        } else if (packet.size < 20) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            incomingPackets[++packetSequence] = packet
            ResponseStatus.INCOMPLETE
        }
    }

    private fun parse(): R2HeartRateModel? {
        var combinedPackets = ByteArray(0)
        val cal: Calendar = Calendar.getInstance()
        val dataHeader1 = incomingPackets[1] ?: return null
        val totalLength = byteArrayOf(dataHeader1[9], dataHeader1[8]).toInt()
        if (totalLength <= 0) {
            return null
        }
        incomingPackets.forEach {
            if (it.key == 1) {
                cal.apply {
                    set(
                        byteArrayOf(it.value[4], it.value[5]).toInt(false),
                        it.value[6].asInt() - 1,
                        it.value[7].asInt()
                    )
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                combinedPackets = byteMerge(combinedPackets, byteCopy(it.value, 10, it.value.size))
            } else {
                combinedPackets = byteMerge(
                    combinedPackets,
                    byteCopy(it.value, 0, it.value.size)
                )
            }
        }
        val details: TreeMap<Int, Int> = TreeMap()
        combinedPackets.forEachIndexed { index, byte ->
            if (index <= 287) {
                details[index] = byte.asInt()
            }
        }

        return R2HeartRateModel(cal.time, details)
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    private fun Calendar.getCurrentMinutes(): Int {
        return this[Calendar.HOUR_OF_DAY] * 60 + this[Calendar.MINUTE]
    }

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }
}

data class R2HeartRateModel(
    var date: Date,
    var details: TreeMap<Int, Int>
):CoreHeartRate