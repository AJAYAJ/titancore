package titan.core.products.reflex_3

import com.google.gson.Gson
import com.titan.logger.coreLogger
import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.*
import titan.core.products.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

private const val commandId: Byte = 8
private const val keyId: Byte = 5
private const val endKey: Byte = -18

class R3StepsHistoryCommand : DataCommand {
    private var incomingPackets = TreeMap<Int, ByteArray>()
    private var missingPackets: ArrayList<Byte> = arrayListOf()
    private var packetSequence = 0
    private var listener: DataCallback<CoreSteps>? = null

    fun callback(listener: DataCallback<CoreSteps>): R3StepsHistoryCommand {
        this.listener = listener
        return this
    }

    fun getData(): R3StepsHistoryCommand {
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
                /*End Command received check for missing packets or end command*/
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
        if (incomingPackets.containsKey(1) && incomingPackets.containsKey(2) && (incomingPackets[1]?.size ?: 0 >= 13) && (incomingPackets[2]?.size ?: 0 >= 20)) {
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

    fun parse() {
        var combinedBytes = ByteArray(0)
        incomingPackets.forEach { (key, value) ->
            if (key > 2) {
                combinedBytes =
                        byteMerge(combinedBytes, byteCopy(value, 4, value[3].toInt()))
            }
        }
        val header1 = incomingPackets[1]
        val header2 = incomingPackets[2]
        if (header1 != null && header1.size >= 13 && header2 != null && header2.size >= 20) {
            try{
                coreLogger("Steps : ${Gson().toJson(incomingPackets.values)}")
            }catch (e:Exception){
                e.printStackTrace()
            }
            if (byteArrayOf(
                            header1[4],
                            header1[5]
                    ).toInt() != 0 && header1[6].asInt() != 0 && header1[7].asInt() != 0
            ) {
                val model = R3Steps(
                        date = getDate(incomingPackets[1]),
                        offset = byteArrayOf(header1[8], header1[9]).toInt(),
                        slotDuration = header1[10].asInt(),
                        headerNumber = header1[11].asInt(),
                        packets = header1[12].asInt(),
                        totalSteps = byteArrayOf(
                                header2[4],
                                header2[5],
                                header2[6],
                                header2[7]
                        ).toLong(),
                        totalCalories = byteArrayOf(
                                header2[8],
                                header2[9],
                                header2[10],
                                header2[11]
                        ).toLong(),
                        totalDistance = byteArrayOf(
                                header2[12],
                                header2[13],
                                header2[14],
                                header2[15]
                        ).toLong(),
                        totalActiveTime = byteArrayOf(
                                header2[16],
                                header2[17],
                                header2[18],
                                header2[19]
                        ).toLong(),
                        stepsDetails = slotDataParse(combinedBytes)
                )
                listener?.onResult(Response.Result(model))
            } else {
                listener?.onResult(Response.Status(true))
            }
        } else {
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

    private fun slotDataParse(data: ByteArray): TreeMap<Int, StepsSlotData> {
        val items: TreeMap<Int, StepsSlotData> = TreeMap()
        var value: String
        for ((index, i) in (0..data.size - 6 step 6).withIndex()) {
            value = data[i + 5].convertToBinaryString() + data[i + 4].convertToBinaryString() + data[i + 3].convertToBinaryString() + data[i + 2].convertToBinaryString() + data[i + 1].convertToBinaryString() + data[i].convertToBinaryString()
            items[index] = StepsSlotData(
                    mode = SportsMode.fromInt(value.substring(46, 48).toInt()),
                    steps = value.substring(34, 46).toDecimal(),
                    activeTime = value.substring(30, 34).toDecimal(),
                    calories = value.substring(20, 30).toDecimal(),
                    distance = value.substring(8, 20).toDecimal(),
                    isBandWorn = value.substring(0, 8).toDecimal() == 1L
            )
        }
        return items
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }
}