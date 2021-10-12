package titan.core.products.reflex_3


/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */
import titan.core.*
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * Serial Number 1:
09-06-01-0D-14-03-03-0B-19-25-0B-05-00-0B-00-04-00-00-00-00
 * byteArrayOf(9,6,1,13,20,3,11,11,23,58,32,5,0,32,0,5,0,0,0,0),
Position 0 : Command ID
Position 1 : Key ID
Position 2 : Serial Number
Position 3 : Current Data Packet Length
Position 4 : Year
Position 5 : Month
Position 6 : Day
Position 7 : Hour
Position 8 : Min
Position 9: Seconds
Position 10: Data Length
Position 11&12: automatic heart rate time Interval (2byte)-
Position 13&14: The Number of Heart Rate
Position 15&16: The Number of Packets
 */
/**
Serial Number 2:
09-06-02-0B-02-00-00-CC-02-00-00-00-0A-00-00-00-00-00-00-00
Position 0 : Command ID
Position 1 : Key ID
Position 2 : Serial Number
Position 3 : Current Data Packet Length
Position 4 : Motion Type
Next 12 bytes will define the -> Steps(18 bits) - Duration(20 bits) - calories(18 bits) - distance(18 bits) i.e(00-00-CC-02-00-00-00-0A-00-00-00-00)
 */
/** Serial Number 3:
09-06-03-08-46-00-00-00-00-00-00-00-00-00-00-00-00-00-00-00
Position 0 : Command ID
Position 1 : Key ID
Position 2 : Serial Number
Position 3 : Length of current packet
Position 4 : Avg Heart Rate
Position 5 : Maximum Heart Rate value
Position6*7: Fat burning Time min
Position8&9: Aerobic exercise min
Position10*11: Extreme exercise time
 */
/** Serial Number 3>0:
09-06-04-0B-46-46-46-46-46-46-46-46-46-46-46-FF-FF-FF-FF-FF
Position 0 : Command ID
Position 1 : Key ID
Position 2 : Serial Number
Position 3 : Current Packet length
Position 4 on wards every two byte value will define "time offset of heart rate data" and "heart rate value"
 */

private const val commandId: Byte = 9
private const val keyId: Byte = 6
private const val endKey: Byte = 0
private const val endKey_2: Byte = 1

class R3MultiSportCommand : DataCommand{
    private var incomingPackets = TreeMap<Int, ByteArray>()
    private var missingPackets: ArrayList<Byte> = arrayListOf()
    private var packetSequence = 0
    private var listener: DataCallback<CoreMultiSport>? = null

    fun get() : R3MultiSportCommand{
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_HEALTH_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(commandId, keyId, 1),
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
                data = byteArrayOf(commandId, keyId, 0, 1),
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
                key = null
            )
        }
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        if (byteArray.isEmpty()) {
            return ResponseStatus.INCOMPATIBLE
        } else if (byteArray[0] != commandId) {
            return ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            return ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && byteArray[1] == keyId && byteArray[2] == endKey) {
            return if (incomingPackets.size == 0) {
                listener?.onResult(Response.Status(true))/*No Data for the day*/
                ResponseStatus.COMPLETED
            } else {
                /*End Command received check for missing packets or end command*/
                checkForMissingPackets()
            }
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
        if (incomingPackets.containsKey(1) && incomingPackets.containsKey(2) && (incomingPackets[1]?.size ?: 0 >= 17) && (incomingPackets[2]?.size ?: 0 >= 15)) {
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
            if (key > 3) {
                combinedBytes = byteMerge(combinedBytes, byteCopy(value, 4, value[3].toInt()))
            }
        }
        val header1 = incomingPackets[1]
        val header2 = incomingPackets[2]
        val header3 = incomingPackets[3]
        if (header1 != null && header1.size >= 17 && header2 != null && header2.size >= 15 && header3!=null && header3.size>=12) {
            val summaryBuilder = StringBuilder()
            incomingPackets[2]?.let { byteArray ->
                for (i in 14 downTo 5) {
                    summaryBuilder.append(byteArray[i].convertToBinaryString())
                }
            }
            val length: Int = summaryBuilder.toString().length
            val model = R3MultiSport(
                date = getDate(header1),
                dataIntervalInMins = 5,// TODO fix this
                hrIntervalInSec = byteArrayOf(header1[11], header1[12]).toInt(),
                hrCount = byteArrayOf(header1[13], header1[14]).toInt(),
                packets = byteArrayOf(header1[15], header1[16]).toInt(),
                motionType = SportsType.reflex3Conversion(header2[4].asInt()),
                steps = summaryBuilder.substring(length - 18, length).toDecimal(),
                duration = summaryBuilder.substring(length - 18 - 20, length - 18).toDecimal(),
                calories = summaryBuilder.substring(length - 18 - 20 - 18, length - 18 - 20).toDecimal(),
                distance = summaryBuilder.substring(length - 18 - 20 - 18 - 18, length - 18 - 20 - 18).toDecimal(),
                avgHR = header3[4].asInt(),
                maxHR = header3[5].asInt(),
                fatBurningTime = byteArrayOf(header3[6], header3[7]).toInt(),
                aerobicExerciseTime = byteArrayOf(header3[8], header3[9]).toInt(),
                extremeExerciseTime = byteArrayOf(header3[10], header3[11]).toInt(),
                details = slotDataParse(combinedBytes)
            )
            println(model)
            listener?.onResult(Response.Result(model))
        }else{
            listener?.onResult(Response.Status(true))
        }
    }

    private fun getDate(data: ByteArray?): Date {
        val calender = getLocalCalendar()
        data?.let {
            calender.set(
                2000+data[4].asInt(),
                data[5].asInt() - 1,
                data[6].asInt(),
                data[7].asInt(),
                data[8].asInt(),
                data[9].asInt()
            )
            calender.set(Calendar.MILLISECOND, 0)
        }
        return calender.time
    }

    private fun slotDataParse(data: ByteArray): TreeMap<Int,Int> {
        val items: TreeMap<Int, Int> = TreeMap()
        for (i in data.indices) {
            if (data[i].asInt()>0){
                items[i] = data[i].asInt()
            }
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

    fun callback(listener: DataCallback<CoreMultiSport>): R3MultiSportCommand {
        this.listener = listener
        return this
    }
}

data class R3MultiSport(
    val date: Date,
    val dataIntervalInMins: Int,
    val hrIntervalInSec: Int,
    val hrCount: Int,
    val packets: Int,
    val motionType: SportsType,
    val steps: Long,
    val duration: Long,
    val calories: Long,
    val distance: Long,
    val avgHR: Int,
    val maxHR: Int,
    val fatBurningTime: Int,
    val aerobicExerciseTime: Int,
    val extremeExerciseTime: Int,
    val details: TreeMap<Int, Int> = TreeMap()
): CoreMultiSport

