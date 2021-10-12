package titan.core.products.reflex_3

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

import titan.core.*
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/**
 * For Header 1 Data Positions different in document and actual response length,
Sleep Data Header1 (08-04-01-10-E4-07-02-14-07-00-A6-01-0D-04)
Position0 - > Command_id
Position1 - > key_id
Position2 - > Serial Number
Position3 - > Valid Data Length
Position4 & 5 - > Year
Position6- > Month
Position7- > Date
Position8 -> sleepEndedTimeH -> 07
Position9 -> sleepEndedTimeM -> 00
Position10&11 -> totalSleepMinutes -> (A6-01) -> 422
Position12 -> sleep item sleep data header after sleep item number -> 0D
Position13 -> packets total packets
 */
/**
 * Sleep Data Header2 (08-04-02-10-0F-07-01-F9-00-83-00-00-1B-13)
Position0 - > Command_id
Position1 - > key_id
Position2 - > Serial Number
Position3 - > Valid Data Length
Position4 - > light_sleep_count
Position5- > deep_sleep_count
Position6- > awake_count
Position7&8 -> light_sleep_min's
Position9&10 -> deep_sleep_min's
 */

private const val commandId: Byte = 8
private const val keyId: Byte = 4
private const val endKey: Byte = -18

class R3SleepCommand : DataCommand {
    private var incomingPackets = TreeMap<Int, ByteArray>()
    private var missingPackets: ArrayList<Byte> = arrayListOf()
    private var packetSequence = 0
    private var listener: DataCallback<CoreSleep>? = null

    fun get(): R3SleepCommand {
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

    private fun checkForMissingPackets(): ResponseStatus {
        if (incomingPackets.containsKey(1) && incomingPackets.containsKey(2) && (incomingPackets[1]?.size ?: 0 >= 14) && (incomingPackets[2]?.size ?: 0 >= 11)) {
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


    private fun parse() {
        var combinedBytes = ByteArray(0)
        incomingPackets.forEach { (key, value) ->
            if (key > 2) {
                combinedBytes = byteMerge(combinedBytes, byteCopy(value, 4, value[3].toInt()))
            }
        }
        val header1 = incomingPackets[1]
        val header2 = incomingPackets[2]
        if (header1 != null && header1.size >= 14 && header2 != null && header2.size >= 11) {
            if (byteArrayOf(header1[4], header1[5]).toInt() != 0 && header1[6].asInt() != 0 && header1[7].asInt() != 0) {
                val date = getDate(header1).atStartOfDay()
                val totalSleepMin = byteArrayOf(header1[10], header1[11]).toInt()
                val sleepEndingCalendar = getLocalCalendar().apply {
                    time = date
                }
                sleepEndingCalendar.set(
                    byteArrayOf(header1[4], header1[5]).toInt(),
                    header1[6].asInt() - 1,
                    header1[7].asInt(),
                    header1[8].asInt(),
                    header1[9].asInt(),
                    0
                )
                val sleepStartingCalendar = getLocalCalendar().apply {
                    timeInMillis = sleepEndingCalendar.timeInMillis
                }
                sleepStartingCalendar.add(Calendar.MINUTE, -totalSleepMin)
                val model = R3Sleep(
                    date = date,
                    endHour = header1[8].asInt(),
                    endMinute = header1[9].asInt(),
                    totalMin = totalSleepMin,
                    sleepStartingTime = sleepStartingCalendar.timeInMillis,
                    sleepEndingTime = sleepEndingCalendar.timeInMillis,
                    sleepSlotSize = header1[12].asInt(),
                    packets = header1[13].asInt(),
                    lightCount = header2[4].asInt(),
                    deepCount = header2[5].asInt(),
                    awakeCount = header2[6].asInt(),
                    lightMinutes = byteArrayOf(header2[7], header2[8]).toInt().toLong(),
                    deepMinutes = byteArrayOf(header2[9], header2[10]).toInt().toLong(),
                    details = slotDataParse(sleepEndingCalendar.time, totalSleepMin, combinedBytes)
                )
                listener?.onResult(Response.Result(model))
            } else {
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

    private fun slotDataParse(
        date: Date,
        totalMin: Int,
        data: ByteArray
    ): TreeMap<Long, R3SleepItem> {
        val calendar = getLocalCalendar().apply {
            time = date
        }
        val items: TreeMap<Long, R3SleepItem> = TreeMap()
        calendar.add(Calendar.MINUTE, -totalMin)
        for (i in (0..data.size - 2 step 2)) {
            if (CoreSleepMode.reflex3Conversion(data[i].asInt())!=CoreSleepMode.NONE){
                items[calendar.timeInMillis] = R3SleepItem(
                    duration = data[i + 1].asInt(),
                    sleepMode = CoreSleepMode.reflex3Conversion(data[i].asInt())
                )
            }
            calendar.add(Calendar.MINUTE, data[i + 1].asInt())
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

    fun callback(listener: DataCallback<CoreSleep>): R3SleepCommand {
        this.listener = listener
        return this
    }

}

data class R3Sleep(
    val date: Date,
    val endHour: Int,
    val endMinute: Int,
    val sleepStartingTime: Long,
    val sleepEndingTime: Long,
    val totalMin: Int,
    val sleepSlotSize: Int,
    val packets: Int,
    val lightCount: Int,
    val deepCount: Int,
    val awakeCount: Int,
    val lightMinutes: Long,
    val deepMinutes: Long,
    var details: TreeMap<Long, R3SleepItem>
) : CoreSleep

data class R3SleepItem(
    val duration: Int,
    val sleepMode: CoreSleepMode
)