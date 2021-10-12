package titan.core.products.reflex_3

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

import com.google.gson.Gson
import com.titan.logger.coreLogger
import titan.core.*
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
First Response is sports data header 1(Position 13 Reserved filled with zero)
08-03-01-10-E4-07-02-19-00-00-0F-60-22-00
Position0 : Command_Id
Position1 : Key_id
Position2 : Serial Number
Position3 : Effective length of package
Position4,5 : Year
Position6 : Month
Position7 : Day
Position8,9 : offset From 0 o'clock every day offset by minute
Position10 :per minutes Generate a data every few minutes
Position11: sport item sport data header after sport item number
Position12: Total item total packages
Position13: Reserved filled with zero
 */
/**
Second Response is sports data header 2
08-03-02-10-35-00-00-00-02-00-00-00-27-00-00-00-31-00-00-00
Position0 : Command_Id
Position1 : Key_id
Position2 : Serial Number
Position3 : Effective length of package
Position4,5,6,7 : Total Steps
Position8,9,10,11 : Total Calories
Position12,13,14,15 : Total Distance
Position16,17,18,19 : Total active time
 */
/**
Third Response onwards sports item each 5 bytes are one set
Position 3 will tell the actual length, so based on length we have to take the binary values
We have to convert this each 5 bytes into binary format.
For Example take 16th serial number second sports item (D4-40-08-70-02). Since Reading format is Right to left data is 02 70 08 40 D4
So binary format is 0000 0010 0111 0000 0000 1000 0100 0000 1101 0100
->Last 2bits are mode
->last 12 bits after mode parameter is step count data value is (00 0000 1101 01) after converting this into decimal format value is 53 cross checked with IDO SDK Data
->next 4 bits after step count parameter is active time  data value is (00 01)
->next 10 bits after active time parameter is calories data value is (0111 0000 00)
->next 12 bits after active time is distance data value is (0000 0010 0111)
00 00 00 00 00 00
 */
/**
 * Positions
 * 0 - > Serial Number
 * 1 - > Key ID
 * 2 - > Serial Number
 */

private const val commandId: Byte = 8
private const val keyId: Byte = 3
private const val endKey: Byte = -18

class R3StepsCommand : DataCommand {
    private var incomingPackets = TreeMap<Int, ByteArray>()
    private var missingPackets: ArrayList<Byte> = arrayListOf()
    private var packetSequence = 0
    private var listener: DataCallback<CoreSteps>? = null

    fun callback(listener: DataCallback<CoreSteps>): R3StepsCommand {
        this.listener = listener
        return this
    }

    fun get(): R3StepsCommand {
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
                    data = byteArrayOf(commandId, keyId, 2, 0),
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
                    responseWillNotify = true,
                    data = byteArrayOf(commandId, keyId, 3, index),
                    key = getKey()
            )
        }
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
                combinedBytes = byteMerge(combinedBytes, byteCopy(value, 4, value[3].toInt()))
            }
        }
        val header1 = incomingPackets[1]
        val header2 = incomingPackets[2]
        if (header1 != null && header1.size >= 13 && header2 != null && header2.size >= 20) {
            try {
                coreLogger("Steps : ${Gson().toJson(incomingPackets.values)}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (byteArrayOf(
                            header1[4],
                            header1[5]
                    ).toInt() != 0 && header1[6].asInt() != 0 && header1[7].asInt() != 0
            ) {
                val model = R3Steps(
                        date = getDate(header1),
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
                listener?.onResult(
                        Response.Result(
                                model
                        )
                )
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
        val calendar: Calendar = getLocalCalendar()
        for ((index, i) in (0..data.size - 6 step 6).withIndex()) {
            /*println("\n=================================Index-$index============================\n")
            println("15 Min Slot Byte Data" + data[i + 5] + ", " + data[i + 4] + ", " + data[i + 3] + ", " + data[i + 2] + ", " + data[i + 1] + ", " + data[i])*/
            value = data[i + 5].convertToBinaryString() + data[i + 4].convertToBinaryString() + data[i + 3].convertToBinaryString() + data[i + 2].convertToBinaryString() + data[i + 1].convertToBinaryString() + data[i].convertToBinaryString()
            /*println("15 Min Slot Binary Value:$value")
            println("Mode : Binary = ${value.substring(46, 48)} ,Int Value= ${SportsMode.fromInt(value.substring(46, 48).toInt())}")
            println("Steps : Binary = ${value.substring(34, 46)} ,Int Value= ${value.substring(34, 46).toDecimal()}")
            println("Active Time: Binary = ${value.substring(30, 34)} ,Int Value= ${value.substring(30, 34).toDecimal()}")
            println("Calories : Binary = ${value.substring(20, 30)} ,Int Value= ${value.substring(20, 30).toDecimal()}")
            println("Distance : Binary = ${value.substring(8, 20)} ,Int Value= ${value.substring(8, 20).toDecimal()}")
            println("Wearing Status : Binary = ${value.substring(0, 8)} ,Int Value= ${value.substring(0, 8).toDecimal()}")
            println("Data :" + Gson().toJson(StepsSlotData(
                    mode = SportsMode.fromInt(value.substring(46, 48).toInt()),
                    steps = value.substring(34, 46).toDecimal(),
                    activeTime = value.substring(30, 34).toDecimal(),
                    calories = value.substring(20, 30).toDecimal(),
                    distance = value.substring(8, 20).toDecimal(),
                    isBandWorn = value.substring(0, 8).toDecimal() == 1L
            )).toString())*/
            items[index] = StepsSlotData(
                    mode = SportsMode.fromInt(value.substring(46, 48).toInt()),
                    steps = value.substring(34, 46).toDecimal(),
                    activeTime = value.substring(30, 34).toDecimal(),
                    calories = value.substring(20, 30).toDecimal(),
                    distance = value.substring(8, 20).toDecimal(),
                    isBandWorn = value.substring(0, 8).toDecimal() == 1L
            )
            /*println("\n=============================================================")*/
        }
        return items
    }
}

data class R3Steps(
        val date: Date,
        val offset: Int,
        val slotDuration: Int,
        val headerNumber: Int,
        val packets: Int,
        val totalSteps: Long,
        val totalCalories: Long,
        val totalDistance: Long,
        val totalActiveTime: Long,
        val stepsDetails: TreeMap<Int, StepsSlotData> = TreeMap()
) : CoreSteps

data class StepsSlotData(
        var mode: SportsMode,
        var steps: Long,
        var activeTime: Long,
        var calories: Long,
        var distance: Long,
        var isBandWorn: Boolean
)

enum class SportsMode {
    NONE,
    SILENT,
    MILD,
    MEDIUM_ACTIVE,
    INTENSE;

    companion object {
        fun fromInt(number: Int): SportsMode {
            return when (number) {
                0 -> SILENT
                1 -> MILD
                16 -> MEDIUM_ACTIVE
                17 -> INTENSE
                else -> SILENT
            }
        }
    }
}