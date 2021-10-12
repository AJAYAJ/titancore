package titan.core.products.reflex_2

import titan.core.*
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */
/*
* The First 3 packets are mandatory and length is 20
* 1=>DE+02+01+FE+ calendar year (2Byte) + calendar month (1Byte) + calendar day (1Byte) + total number of steps of the day (4Bytes) + total number of calories of the day (4Bytes) + total distance on the day (2Bytes) + total exercise time on the day (2Bytes)
* 2=>Sleep time of the day (2byte) + rest time on the day (2byte) + Walking time of the day (2byte) + time of walking at a slow pace of the day (2byte) + time of walking at a medium pace of the day (2byte) + time of walking at a fast pace of the day (2byte) + time of running at a slow pace of the day (2byte)+ time of running at a medium pace of the day (2byte) + time of running at a fast pace of the day (2byte) + the device’s electric quantity (1byte) +00
* 3=>The device’s latest step distance of the day (2Byte) +the device’s latest weight of the day (2Byte) + the device’s latest step counting target of the day (3Byte) + target sleep time (2byte) + total byte number of the data to be transmitted of the day (2Bytes) +00+00+00+00+00+00+00+00+00
* Fourth Packet onwards we will get step and sleep.
* If the data is related to current day ==> we need to consider every 3 bytes as one set. 1 st byte will indicate time serial number, second byte will tell whether the data is related to sleep or step
* If 2nd byte starts with 0X80 series then the data is related to sleep. If 2nd byte starts with zero then the data is related to steps
* If data is related to steps then steps count will be steps = (byte[1],byte[2]).toInt()
* If the data is related to past day==> For steps we need to consider 3 bytes and for sleep we need to consider 2 bytes
* Time serial number will has only one byte length so max is 255, If time serial number crossed 255 index then again it starts with 0.
* */
private const val command: Byte = -66
private const val id: Byte = 2
private const val key: Byte = 1
private const val endKey: Byte = -34

class R2StepsSleepCommand : DataCommand {
    private var listener: DataCallback<CoreSteps>? = null
    private var incomingPackets = TreeMap<Int, ByteArray>()
    private var packetSequence = 0

    fun get(date: Date): R2StepsSleepCommand {
        val data = byteArrayOf(
            command,
            id,
            key,
            -2,
            (date.toYear() shr 8).toByte(),
            date.toYear().toByte(),
            date.toMonth().toByte(),
            date.toDay().toByte(),
            0,
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

    fun callback(listener: DataCallback<CoreSteps>): R2StepsSleepCommand {
        this.listener = listener
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
                packetSequence = 0
                val data = parse()
                if (data != null) {
                    listener?.onResult(Response.Result(data))
                } else {
                    listener?.onResult(Response.Status(false))
                }
                ResponseStatus.COMPLETED
            } else {
                incomingPackets[++packetSequence] = packet
                ResponseStatus.INCOMPLETE
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

    private fun parse(): R2StepsSleepData? {
        val header1 = incomingPackets[1]
        val header2 = incomingPackets[2]
        val header3 = incomingPackets[3]
        if (header1 != null && header1.size >= 20 && header2 != null && header2.size >= 20
            && header3 != null && header3.size >= 20
        ) {
            var combinedBytes = ByteArray(0)
            val totalPackets: Int = byteArrayOf(header3[9], header3[10]).toInt(false)
            incomingPackets.forEach { (key, value) ->
                if (key > 3) {
                    combinedBytes =
                        byteMerge(
                            combinedBytes,
                            byteCopy(
                                value,
                                0,
                                if ((combinedBytes.size + value.size) > totalPackets)
                                    totalPackets - combinedBytes.size
                                else
                                    value.size
                            )
                        )
                }
            }
            if (combinedBytes.isNotEmpty() && combinedBytes.size >= totalPackets) {
                val stepsSleepDetails = slotDataParse(combinedBytes, getDate(header1))
                val cal: Calendar = Calendar.getInstance()
                return R2StepsSleepData(
                    date = cal.apply {
                        set(
                            byteArrayOf(header1[4], header1[5]).toInt(false),
                            header1[6].asInt() - 1,
                            header1[7].asInt()
                        )
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time,
                    steps = byteArrayOf(
                        header1[8],
                        header1[9],
                        header1[10],
                        header1[11]
                    ).toLong(false),
                    calories = byteArrayOf(
                        header1[12],
                        header1[13],
                        header1[14],
                        header1[15]
                    ).toLong(false),
                    distance = byteArrayOf(header1[16], header1[17]).toLong(false) * 10,
                    activeTime = byteArrayOf(header1[18], header1[19]).toLong(false) * 60,
                    sleepTime = byteArrayOf(header2[0], header2[1]).toLong(false),
                    restTime = byteArrayOf(header2[2], header2[3]).toLong(false),
                    walkingTime = byteArrayOf(header2[4], header2[5]).toLong(false),
                    slowWalkingTime = byteArrayOf(header2[6], header2[7]).toLong(false),
                    mediumWalkingTime = byteArrayOf(header2[8], header2[9]).toLong(false),
                    fastWalkingTime = byteArrayOf(header2[10], header2[11]).toLong(false),
                    slowRunningTime = byteArrayOf(header2[12], header2[13]).toLong(false),
                    mediumRunningTime = byteArrayOf(header2[14], header2[15]).toLong(false),
                    fastRunningTime = byteArrayOf(header2[16], header2[17]).toLong(false),
                    deviceBattery = header2[18].asInt(),
                    stepsDistance = byteArrayOf(header3[0], header3[1]).toLong(false),
                    weight = byteArrayOf(header3[2], header3[3]).toLong(false),
                    stepsTarget = byteArrayOf(header3[4], header3[5], header3[6], 0).toLong(false),
                    sleepTarget = byteArrayOf(header3[7], header3[8]).toLong(false),
                    totalSlots = byteArrayOf(header3[9], header3[10]).toLong(false) / 3,
                    totalPackets = byteArrayOf(header3[9], header3[10]).toLong(false),
                    sleepDetails = stepsSleepDetails.first,
                    stepsDetails = stepsSleepDetails.second
                )
            }
        }
        return null
    }

    private fun getDate(data: ByteArray): Date {
        val calendar = Calendar.getInstance()
        calendar.set(
            byteArrayOf(data[4], data[5]).toInt(false),
            data[6].asInt() - 1,
            data[7].asInt(),
            0,
            0,
            0
        )
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun slotDataParse(
        data: ByteArray,
        date: Date
    ): Pair<TreeMap<Int, CoreSleepMode>, TreeMap<Int, Long>> {
        val calendar: Calendar = getLocalCalendar()
        val currentMinute = if (calendar[Calendar.MINUTE] % 5 == 0) {
            (calendar[Calendar.HOUR_OF_DAY] * 60 + calendar[Calendar.MINUTE]) / 5
        } else {
            ((calendar[Calendar.HOUR_OF_DAY] * 60 + calendar[Calendar.MINUTE]) / 5) + 1
        }
        val isToday = calendar.time.atStartOfDay() == date.atStartOfDay()
        calendar.time = date.atStartOfDay()
        val sleepItems: TreeMap<Int, CoreSleepMode> = TreeMap()
        val stepsItems: TreeMap<Int, Long> = TreeMap()
        var dataProcessedLength = 0
        var lastIndex = 0
        while (dataProcessedLength < data.size) {
            var index = data[dataProcessedLength].asInt()
            if (index < lastIndex) {
                index += 255
            }
            if (lastIndex > index) {
                break
            }
            if (isToday) {
                if ((dataProcessedLength + 2) >= data.size) {
                    break
                }
                if (index <= currentMinute) {
                    val sleepMode = if (data[dataProcessedLength + 1].asInt() and 0x0080 == 128) {
                        CoreSleepMode.reflex2Conversion(data[dataProcessedLength + 1].asInt())
                    } else {
                        CoreSleepMode.NONE
                    }
                    sleepItems[index] = sleepMode
                    stepsItems[index] = if (sleepMode == CoreSleepMode.NONE) byteArrayOf(
                        data[dataProcessedLength + 1],
                        data[dataProcessedLength + 2]
                    ).toLong(false) else 0
                }
                dataProcessedLength += 3
            } else {
                val sleepMode = if (data[dataProcessedLength + 1].asInt() and 0x0080 == 128) {
                    CoreSleepMode.reflex2Conversion(data[dataProcessedLength + 1].asInt())
                } else {
                    CoreSleepMode.NONE
                }

                if (dataProcessedLength + 1 >= data.size) {
                    break
                } else if (sleepMode == CoreSleepMode.NONE) {
                    if ((dataProcessedLength + 2) >= data.size) {
                        break
                    }
                }

                if (index == 0 || index == lastIndex + 1) {
                    sleepItems[index] = sleepMode
                    stepsItems[index] = if (sleepMode == CoreSleepMode.NONE) byteArrayOf(
                        data[dataProcessedLength + 1],
                        data[dataProcessedLength + 2]
                    ).toLong(false) else 0
                } else {
                    for (i in lastIndex + 1..index) {
                        sleepItems[i] = sleepMode
                        stepsItems[i] = if (sleepMode == CoreSleepMode.NONE) byteArrayOf(
                            data[dataProcessedLength + 1],
                            data[dataProcessedLength + 2]
                        ).toLong(false) else 0
                    }
                }
                dataProcessedLength += if (sleepMode != CoreSleepMode.NONE) {
                    2
                } else {
                    3
                }
            }
            lastIndex = index
        }
        return Pair(sleepItems, stepsItems)
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }
}
