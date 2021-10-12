package titan.core.products.reflex_slay

import com.google.gson.Gson
import titan.core.*
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

private const val messageId: Byte = 64
private const val messageLength: Byte = 1
private const val endKey: Byte = 2
private const val endId: Byte = -1

internal class RSGetDailyRecordCommand : DataCommand {
    private var listener: DataCallback<CoreSteps>? = null
    private var incomingPackets = TreeMap<Int, ByteArray>()
    private var packetSequence = 0

    fun get(dayNumber: Int = 0): RSGetDailyRecordCommand {
        val data = byteArrayOf(
            messageLength,
            messageId,
            dayNumber.toByte()
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

    fun callback(listener: DataCallback<CoreSteps>): RSGetDailyRecordCommand {
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

    private fun getDate(data: ByteArray): Date {
        val calendar = Calendar.getInstance()
        calendar.set(
            byteArrayOf(data[5], data[6]).toInt(false),
            data[7].asInt() - 1,
            data[8].asInt(),
            0,
            0,
            0
        )
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun parse(): RSDailyRecord? {
        val header1 = incomingPackets[2]
        if (header1?.size == 20) {
            var combinedBytes = ByteArray(0)
            incomingPackets.forEach { (key, value) ->
                if (key > 2) {
                    combinedBytes =
                        byteMerge(combinedBytes, byteCopy(value, 1, value.size))
                }
            }
            if (combinedBytes.isNotEmpty()) {
                val stepsSleepHRDetails = slotDataParse(combinedBytes, date = getLocalCalendar().apply {
                    set(
                        byteArrayOf(header1[6], header1[5]).toInt(false),
                        header1[7].asInt() - 1,
                        header1[8].asInt()
                    )
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time)
                val cal: Calendar = Calendar.getInstance()
                return RSDailyRecord(
                    serialNumber = byteArrayOf(
                        header1[1],
                        header1[2],
                        header1[3],
                        header1[4]
                    ).toInt(false),
                    date = cal.apply {
                        set(
                            byteArrayOf(header1[6], header1[5]).toInt(false),
                            header1[7].asInt() - 1,
                            header1[8].asInt()
                        )
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time,
                    totalSteps = byteArrayOf(
                        header1[11],
                        header1[10],
                        header1[9]
                    ).toLong(false),
                    totalCalories = byteArrayOf(
                        header1[14],
                        header1[13],
                        header1[12]
                    ).toLong(false),
                    totalDistance = byteArrayOf(
                        header1[17],
                        header1[16],
                        header1[15]
                    ).toLong(false),
                    totalSleep = byteArrayOf(
                        header1[18],
                        header1[19]
                    ).toLong(false),
                    sleepDetails = stepsSleepHRDetails.first,
                    stepsDetails = stepsSleepHRDetails.second,
                    hrDetails = stepsSleepHRDetails.third
                )
            }
        }
        return null
    }

    private fun slotDataParse(data: ByteArray, date: Date):
            Triple<TreeMap<Int, CoreSleepMode>, TreeMap<Int, Long>, TreeMap<Int, Long>> {
        val cal = getLocalCalendar().apply { time = date }
        val sleepItems: TreeMap<Int, CoreSleepMode> = TreeMap()
        val stepsItems: TreeMap<Int, Long> = TreeMap()
        val hrItems: TreeMap<Int, Long> = TreeMap()
        for ((index, i) in (0..data.size - 2 step 2).withIndex()) {
           if(index > 1439) {
               return Triple(sleepItems, stepsItems, hrItems)
           }
//            println("index_:$i,$index")
            if (data[i].asInt() >= 250) {
                sleepItems[index] = CoreSleepMode.reflexSlayConversion(data[i].asInt())
                println(cal.time.toString()+ "${CoreSleepMode.reflexSlayConversion(data[i].asInt())}")
            } else {
                stepsItems[index] = data[i].toLong()
            }
            hrItems[index] = data[i + 1].toLong()
            cal.add(Calendar.MINUTE, 1)
        }
        return Triple(sleepItems, stepsItems, hrItems)
    }
}