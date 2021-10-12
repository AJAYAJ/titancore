package titan.core.products.reflex_3

import com.google.gson.Gson
import titan.core.*
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */
private const val command: Byte = 51
private val fixedHeader = byteArrayOf(-38, -83, -38, -83)
private const val version: Byte = 1
private const val dataType: Byte = 4
private const val isToday: Byte = 0
private val cmdID: ByteArray = byteArrayOf(4, 0)
private val syncOffset: ByteArray = byteArrayOf(0, 0)

class R3MultiSportV3Command : DataCommand {
    private var incomingPackets = TreeMap<Int, ByteArray>()
    private var packetSequence = 0
    private var packetLength = 0
    private var listener: DataCallback<CoreMultiSport>? = null

    fun get(): R3MultiSportV3Command {
        val data =
            byteArrayOf(command).plus(fixedHeader).plus(byteArrayOf(version, 16, 0)).plus(cmdID)
                .plus(byteArrayOf(0, 0)).plus(byteArrayOf(0, dataType, isToday)).plus(syncOffset)
        println(data)
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(R3_COMMUNICATION_SERVICE, R3_COMMUNICATION_WRITE_CHAR),
                read = null,
                responseWillNotify = true,
                data = data,
                key = getKey()
            )
        }
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        if (byteArray.isEmpty()) {
            return ResponseStatus.INCOMPATIBLE
        } else if (byteArray[0] != command) {
            return ResponseStatus.INCOMPATIBLE
        } else if (incomingPackets.size >= 1) {
            incomingPackets[++packetSequence] = byteArray
            /*Total packet length will exclude the command. So we need to reduce the 1 length from byte size manually*/
            packetLength -= byteArray.size - 1
            return if (packetLength <= 0) {
                parse()
                ResponseStatus.COMPLETED

            } else {
                ResponseStatus.INCOMPLETE
            }
        } else if (byteArray.size < 8 || !byteCopy(byteArray, 1, 4).contentEquals(fixedHeader)) {
            return ResponseStatus.INCOMPATIBLE
        } else {
            packetLength = byteArrayOf(byteArray[6], byteArray[7]).toInt()
            incomingPackets[++packetSequence] = byteArray
            /*Total packet length will exclude the command. So we need to reduce the 1 length from byte size manually*/
            /*If Packet length is less than 26. No data for the day*/
            packetLength -= byteArray.size - 1
            return if (packetLength <= 0 && byteArray.size<=26) {
                parse()
                ResponseStatus.COMPLETED
            } else {
                ResponseStatus.INCOMPLETE
            }
        }
    }

    fun parse() {
        var combinedBytes = ByteArray(0)
        var header1 = ByteArray(0)
        var activityExData1 = ByteArray(0)
        var activityExData2 = ByteArray(0)
        var hrData = ByteArray(0)
        var rawActivityItemData = ByteArray(0)
        incomingPackets.forEach { (_, value) ->
            combinedBytes = byteMerge(combinedBytes, byteCopy(value, 1, value.size - 1))
        }
        /*78 bytes will contain header of multi sport activity[activity start time, packet length, hr item count, hr interval], activity summary, hr summary.
        * If the value is less than 78 it is invalid data*/
        if (combinedBytes.size < 78) {
            /*endSyncCommand()*/
            listener?.onResult(Response.Status(false))
            return
        }
        header1 = byteMerge(header1, byteCopy(combinedBytes, 26, 18))
        activityExData1 = byteMerge(activityExData1, byteCopy(combinedBytes, 44, 15))
        activityExData2 = byteMerge(activityExData2, byteCopy(combinedBytes, 59, 19))
        /*21 is reserved bytes. these are constant
        Todo:// Validate hr and activity item lengths also
        val validHRItemLength = 78 + 21 + byteArrayOf(header1[14], header1[15]).toInt()
        val validActivityItemLength = 78 + 21 + byteArrayOf(header1[14], header1[15]).toInt() + (byteArrayOf(header1[16], header1[17]).toInt() * 6)*/
        val hrItemCount = byteArrayOf(header1[14], header1[15]).toInt()
        if (hrItemCount > 0) {
            hrData = byteMerge(hrData, byteCopy(combinedBytes, 101, hrItemCount))
        }
        var activityItemCount = 0
        if (combinedBytes.size > 100) {
            activityItemCount = byteArrayOf(combinedBytes[99], combinedBytes[100]).toInt()
        }
        val activityStartingIndex = 101 + hrItemCount
        if (activityItemCount > 0) {
            rawActivityItemData = byteMerge(
                rawActivityItemData,
                byteCopy(combinedBytes, activityStartingIndex, activityItemCount * 6)
            )
        }
        val summaryBinaryString =
            activityExData1[14].convertToBinaryString() + activityExData1[13].convertToBinaryString() + activityExData1[12].convertToBinaryString() + activityExData1[11].convertToBinaryString() + activityExData1[10].convertToBinaryString() + activityExData1[9].convertToBinaryString() + activityExData1[8].convertToBinaryString() + activityExData1[7].convertToBinaryString() + activityExData1[6].convertToBinaryString() + activityExData1[5].convertToBinaryString()
        val parsedItemData = parseActivityData(rawActivityItemData)
        val model = R3MultiSportV3(
            date = getDate(header1),
            hrIntervalInSec = byteArrayOf(header1[12], header1[13]).toInt(),
            activityInterval = 5,
            hrCount = byteArrayOf(header1[14], header1[15]).toInt(),
            packets = 0,
            motionType = SportsType.reflex3Conversion(activityExData1[4].asInt()),
            steps = summaryBinaryString.substring(62, 80).toDecimal(),
            duration = summaryBinaryString.substring(42, 62).toDecimal(),
            calories = summaryBinaryString.substring(24, 42).toDecimal(),
            distance = summaryBinaryString.substring(6, 24).toDecimal(),
            avgHR = activityExData2[4].asInt(),
            maxHR = activityExData2[5].asInt(),
            fatBurningTime = byteArrayOf(activityExData2[6], activityExData2[7]).toInt(),
            aerobicExerciseTime = byteArrayOf(activityExData2[8], activityExData2[9]).toInt(),
            extremeExerciseTime = 0,
            hrDetails = slotHRDataParse(hrData),
            stepsDetails = parsedItemData.stepsDetails,
            calorieDetails = parsedItemData.calorieDetails,
            distanceDetails = parsedItemData.distanceDetails
        )
        println("--------------------------------------------------------------\n")
        println(Gson().toJson(model))
        println("--------------------------------------------------------------\n")
        /*endSyncCommand()*/
        listener?.onResult(Response.Result(model))
    }

    private fun getDate(data: ByteArray?): Date {
        val calender = getLocalCalendar()
        data?.let {
            calender.set(
                byteArrayOf(data[4], data[5]).toInt(),
                data[6].asInt() - 1,
                data[7].asInt(),
                data[8].asInt(),
                data[9].asInt(),
                data[10].asInt()
            )
            calender.set(Calendar.MILLISECOND, 0)
        }
        return calender.time
    }

    private fun slotHRDataParse(
        data: ByteArray
    ): TreeMap<Int, Int> {
        val items: TreeMap<Int, Int> = TreeMap()
        for (i in data.indices) {
            if (data[i].asInt() > 0) {
                items[i] = data[i].asInt()
            }
        }
        return items
    }

    private fun parseActivityData(
        data: ByteArray,
    ): R3MultiSportV3ActivityDetails {
        val items: R3MultiSportV3ActivityDetails
        val steps: TreeMap<Int, Int> = TreeMap()
        val calories: TreeMap<Int, Int> = TreeMap()
        val distance: TreeMap<Int, Int> = TreeMap()
        for ((index, i) in (0..data.size - 6 step 6).withIndex()) {
            steps[index] = byteArrayOf(data[i], data[i + 1]).toInt()
            calories[index] = byteArrayOf(data[i + 2], data[i + 3]).toInt()
            distance[index] = byteArrayOf(data[i + 4], data[i + 5]).toInt()
        }
        items = R3MultiSportV3ActivityDetails(
            stepsDetails = steps,
            calorieDetails = calories,
            distanceDetails = distance
        )
        return items
    }

    private fun endSyncCommand(){
        endSync()
        endSync()
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
                    data = byteArrayOf(3.toByte(),16.toByte(),160.toByte(),136.toByte(),19.toByte(),1.toByte(),208.toByte(),7.toByte(),12.toByte(),11.toByte()),
                    key = null
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

    fun callback(listener: DataCallback<CoreMultiSport>): R3MultiSportV3Command {
        this.listener = listener
        return this
    }
}

data class R3MultiSportV3(
    val date: Date,
    val hrIntervalInSec: Int,
    val activityInterval: Int,
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
    val hrDetails: TreeMap<Int, Int> = TreeMap(),
    val stepsDetails: TreeMap<Int, Int> = TreeMap(),
    val calorieDetails: TreeMap<Int, Int> = TreeMap(),
    val distanceDetails: TreeMap<Int, Int> = TreeMap()
) : CoreMultiSport


data class R3MultiSportV3ActivityDetails(
    val stepsDetails: TreeMap<Int, Int> = TreeMap(),
    val calorieDetails: TreeMap<Int, Int> = TreeMap(),
    val distanceDetails: TreeMap<Int, Int> = TreeMap()
)

