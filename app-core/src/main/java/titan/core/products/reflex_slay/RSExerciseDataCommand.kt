package titan.core.products.reflex_slay

import com.google.gson.Gson
import titan.core.*
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

private const val messageId: Byte = 66
private const val messageLength: Byte = 1
private const val endKey: Byte = 2
private const val endId: Byte = -1

internal class RSExerciseDataCommand : DataCommand {
    private var listener: DataCallback<CoreMultiSport>? = null
    private var incomingPackets = TreeMap<Int, ByteArray>()
    private var packetSequence = 0
    private var hrStartIndex = 0
    private var hrEndIndex = 0
    private var activityStartIndex = 0
    private var activityEndIndex = 0
    private var searchPositionTemp = 0
    private var data: ByteArray = byteArrayOf()
    private var hrItems: ByteArray  = byteArrayOf()
    private var activityItems: ByteArray = byteArrayOf()
    private val hr: TreeMap<Int, Int> = TreeMap()
    private val steps: TreeMap<Int, Int> = TreeMap()
    private val distance: TreeMap<Int, Int> = TreeMap()
    private val calories: TreeMap<Int, Int> = TreeMap()
    fun get(exerciseSerialNumber: Int = 0): RSExerciseDataCommand {
        val data = byteArrayOf(
            messageLength,
            messageId,
            exerciseSerialNumber.toByte()
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

    fun callback(listener: DataCallback<CoreMultiSport>): RSExerciseDataCommand {
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
        return if (packetSequence > 0) {
            when {
                byteArray.size < 20 -> {
                    listener?.onResult(Response.Status(false))
                    ResponseStatus.INVALID_DATA_LENGTH
                }
                byteArray[0] == (-127).toByte() -> {
                    incomingPackets[++packetSequence] = byteArray
                    packetSequence = 0
                    val data = parse()
                    if (data != null) {
                        listener?.onResult(Response.Result(data))
                        println(data)
                    } else {
                        listener?.onResult(Response.Status(false))
                    }
                    ResponseStatus.COMPLETED
                }
                else -> {
                    incomingPackets[++packetSequence] = byteArray
                    ResponseStatus.INCOMPLETE
                }
            }
        } else if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != endId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 3 && byteArray[3].asInt() == 4) {
            /*No Data for the day*/
            listener?.onResult(Response.Status(true))
            ResponseStatus.COMPLETED
        } else if (byteArray.size < 4) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            incomingPackets[++packetSequence] = byteArray
            ResponseStatus.INCOMPLETE
        }
    }

    private fun parse(): RSExerciseData? {
        val header1 = incomingPackets[2]
        val header2 = incomingPackets[3]

        if (header1?.size == 20 && header2?.size == 20) {
            var combinedBytes = ByteArray(0)
            incomingPackets.forEach { (key, value) ->
                if (key > 3) {
                    combinedBytes =
                        byteMerge(combinedBytes, byteCopy(value, 1, value.size))
                }
            }
            if (combinedBytes.isNotEmpty()) {
                data = combinedBytes
                slotDataParse()
//                val hrDetails = TreeMap()
                val cal: Calendar = Calendar.getInstance()
                val a = RSExerciseData(
                    serialNumber = byteArrayOf(
                        header1[0]
                    ).toInt(),
                    sportType = SportsType.reflexSlayConversion(header1[1].asInt()),
                    avgHR = header1[2].asInt(),
                    exerciseStartTime = exerciseStartGetDate(header1),
                    totalExerciseTime = getTotalExerciseTime(header1),
                    exerciseEndTime = exerciseEndGetDate(header1),
                    totalSteps = byteArrayOf(
                        header2[0],
                        header1[19],
                        header1[18]
                    ).toLong(false),
                    avgSpeed = byteArrayOf(
                        header2[4],
                        header2[3],
                        header2[2],
                        header2[1]
                    ).toLong(false),
                    totalDistance = byteArrayOf(
                        header2[8],
                        header2[7],
                        header2[6],
                        header2[5]
                    ).toLong(false),
                    totalCalories = byteArrayOf(
                        header2[12],
                        header2[11],
                        header2[10],
                        header2[9]
                    ).toLong(false),
                    hrDetails = hr,
                    stepsDetails = steps,
                    distanceDetails = distance,
                    calorieDetails = calories
                )
                println(Gson().toJson(a))
                return a
            }
        }
        return null
    }

    private fun getTotalExerciseTime(data: ByteArray?): Long {
        var min: Long = 0
        data?.let {
           min =  ( (data[9].asLong() * 60 + data[10].asLong()) * 60 + data[11].asLong())
        }
        return min
    }
    private fun exerciseStartGetDate(data: ByteArray?): Date {
        val calender = getLocalCalendar()
        data?.let {
            calender.set(
                2000 + data[3].asInt(),
                data[4].asInt() - 1,
                data[5].asInt(),
                data[6].asInt(),
                data[7].asInt(),
                data[8].asInt()
            )
            calender.set(Calendar.MILLISECOND, 0)
        }
        return calender.time
    }

    private fun exerciseEndGetDate(data: ByteArray?): Date {
        val calender = getLocalCalendar()
        data?.let {
            calender.set(
                2000 + data[12].asInt(),
                data[13].asInt() - 1,
                data[14].asInt(),
                data[15].asInt(),
                data[16].asInt(),
                data[17].asInt()
            )
            calender.set(Calendar.MILLISECOND, 0)
        }
        return calender.time
    }

    private fun slotDataParse()  {
         hrEndIndex = slotGapPosition(data, searchPositionTemp) - 1
         activityStartIndex = hrEndIndex + 3
         activityEndIndex = activityStartIndex + 5
        if(data.size > hrStartIndex) {
            println("hrStartIndex: $hrStartIndex")
            println("hrEndIndex: $hrEndIndex")
            println("activityStartIndex: $activityStartIndex")
            println("activityEndIndex: $activityEndIndex")
            println("Exercise:${data.size}")
            hrItems = byteMerge(hrItems, byteCopy(data, hrStartIndex, (hrEndIndex - hrStartIndex) + 1))
            activityItems = byteMerge(activityItems, byteCopy(data, activityStartIndex, 6))
            if (data.size > activityEndIndex + 1 && data[activityEndIndex + 1].asInt() != 255) {
                hrStartIndex = activityEndIndex + 1
                searchPositionTemp = activityEndIndex + 1
                slotDataParse()
            } else {
                parseActivityData()
                return
            }
        } else {
            parseActivityData()

        }
    }

    private fun slotGapPosition(data: ByteArray, startFrom: Int): Int {
        for (i in startFrom..data.size) {
            if (data[i].asInt() == 250) {
                return i
            }
        }
        return 0
    }

    private fun parseActivityData() {
        println(Gson().toJson(hrItems))
        println(Gson().toJson(hrItems.size))
        println(Gson().toJson(activityItems))
        println(Gson().toJson(activityItems.size))
        for (i in hrItems.indices) {
            hr[i] = hrItems[i].asInt()
        }
        for ((index, i) in (0..activityItems.size - 6 step 6).withIndex()) {
            steps[index] = byteArrayOf(activityItems[i], activityItems[i + 1]).toInt()
            distance[index] = byteArrayOf(activityItems[i + 2], activityItems[i + 3]).toInt()
            calories[index] = byteArrayOf(activityItems[i + 4], activityItems[i + 5]).toInt()
        }
    }
}