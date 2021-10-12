package titan.core.products.reflex_3

import com.google.gson.Gson
import titan.core.*
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

private const val commandId: Byte = 8
private const val keyId: Byte = 8
private const val endKey: Byte = -18

class R3HeartRateHistoryCommand : DataCommand {
    private var incomingPackets = TreeMap<Int, ByteArray>()
    private var missingPackets: ArrayList<Byte> = arrayListOf()
    private var packetSequence = 0
    private var listener: DataCallback<CoreHeartRate>? = null
    fun get(): R3HeartRateHistoryCommand {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_HEALTH_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(commandId, keyId, 1, 0),
                key = getKey()
            )
        }
        return this
    }

    private fun endSync() {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_HEALTH_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = false,
                data = byteArrayOf(commandId, keyId, 2),
                key = null
            )
        }
    }

    private fun required(index: Byte) {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_HEALTH_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = false,
                data = byteArrayOf(commandId, keyId, 3, index),
                key = getKey()
            )
        }
    }

    fun callback(listener: DataCallback<CoreHeartRate>): R3HeartRateHistoryCommand {
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

    fun check(byteArray: ByteArray): ResponseStatus {
        if (byteArray.isEmpty()) {
            return ResponseStatus.INCOMPATIBLE
        } else if (byteArray[0] != commandId) {
            return ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] == endKey) {
            return if (incomingPackets.size == 0) {
                listener?.onResult(Response.Status(true))/*No Data for the day*/
                ResponseStatus.COMPLETED
            } else {
                /*End Command received check for missing packets or end send the end command*/
                checkForMissingPackets()
            }
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            return ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 4) {
            listener?.onResult(Response.Status(false))
            return ResponseStatus.INVALID_DATA_LENGTH
        } else {
            incomingPackets[++packetSequence] = byteArray
            if (missingPackets.isNotEmpty()) {
                val missedPacketSerialNumber = missingPackets.indexOf(byteArray[2])
                if (missedPacketSerialNumber != -1) {
                    missingPackets.removeAt(missedPacketSerialNumber)
                }
                if (missingPackets.isEmpty()) {
                    return checkForMissingPackets()
                }
                return ResponseStatus.INCOMPLETE
            } else {
                return ResponseStatus.INCOMPLETE
            }
        }
    }

    private fun checkForMissingPackets(): ResponseStatus {
        if (incomingPackets.containsKey(1) && incomingPackets.containsKey(2) && (incomingPackets[1]?.size ?: 0 >= 14) && (incomingPackets[2]?.size ?: 0 >= 13)) {
            missingPackets = /*missingPackets(
                incomingPackets.keys, incomingPackets[1]?.get(12)?.asInt() ?: incomingPackets.size
            )*/arrayListOf()
            return when {
                missingPackets.isNullOrEmpty() -> {
                    parse()
                    endSync()
                    ResponseStatus.COMPLETED
                }
                missingPackets.isNotEmpty() -> {
                    for (packet in missingPackets) {
                        required(packet)
                    }
                    ResponseStatus.ITEM_MISSED
                }
                else -> {
                    ResponseStatus.INCOMPATIBLE
                }
            }
        } else {
            listener?.onResult(Response.Status(false))
            return ResponseStatus.INVALID_DATA_LENGTH
        }
    }

    private fun parse() {
        var combinedBytes = ByteArray(0)
        incomingPackets.forEach { (key, value) ->
            if (key > 2) {
                combinedBytes = byteMerge(combinedBytes, byteCopy(value, 4, value[3].toInt()))
            }
        }
        val header1 = incomingPackets[1]
        val header2 = incomingPackets[2]
        if (header1 != null && header1.size >= 14 && header2 != null && header2.size >= 13) {
            if (byteArrayOf(
                    header1[4],
                    header1[5]
                ).toInt() != 0 && header1[6].asInt() != 0 && header1[7].asInt() != 0
            ) {
                val date = getDate(header1).atStartOfDay()
                val model = R3HeartRateModel(
                    date = date,
                    offset = byteArrayOf(header1[8], header1[9]).toInt(),
                    silentHR = header1[10].asInt(),
                    totalCount = byteArrayOf(header1[11], header1[12]).toInt(),
                    totalPackets = header1[13].asInt(),
                    maxHR = 0,
                    burnFatThreshold = header2[4].asInt(),
                    aerobicThreshold = header2[5].asInt(),
                    limitExtremeExerciseThreshold = header2[6].asInt(),
                    burnFatMinutes = byteArrayOf(header2[7], header2[8]).toInt(),
                    aerobicMinutes = byteArrayOf(header2[9], header2[10]).toInt(),
                    limitExerciseMinutes = byteArrayOf(header2[11], header2[12]).toInt(),
                    details = slotDataParse(
                        date = date,
                        offset = byteArrayOf(header1[8], header1[9]).toInt(),
                        data = combinedBytes
                    )
                )
                listener?.onResult(
                    Response.Result(
                        model
                    )
                )
            }else{
                listener?.onResult(Response.Status(true))
            }
        }else{
            listener?.onResult(Response.Status(true))
        }
    }

    private fun getDate(data: ByteArray?): Date {
        val calender = getLocalCalendar()
        data?.let {
            calender.set(
                byteArrayOf(data[4], data[5]).toInt(),
                data[6].asInt() - 1,
                data[7].asInt(),
                0,
                0,
                0
            )
            calender.set(Calendar.MILLISECOND, 0)
        }
        return calender.time
    }

    private fun slotDataParse(date: Date, offset: Int, data: ByteArray): TreeMap<Long, Int> {
        val calendar = getLocalCalendar().apply {
            time = date
        }
        val items: TreeMap<Long, Int> = TreeMap()
        calendar.add(Calendar.MINUTE, offset)
        for (i in (0..data.size - 2 step 2)) {
            calendar.add(Calendar.MINUTE, data[i].asInt())
            if (data[i + 1].asInt() > 0) {
                items[calendar.timeInMillis] = data[i + 1].asInt()
            }
        }
        return items
    }
}