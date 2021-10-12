package titan.core.products.reflex_2

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

private const val command: Byte = -66
private const val id: Byte = 1
private const val key: Byte = 8
private const val endKey: Byte = -34

internal class R2DisplayInterfaceCommand : DataCommand {
    private var listener: DataCallback<CoreDisplayInterface>? = null

    fun get(): R2DisplayInterfaceCommand {
        val data = byteArrayOf(command, id, key, -19)
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

    fun set(displayInterface: CoreDisplayInterface): R2DisplayInterfaceCommand {
        val data = ByteArray(20)
        data[0] = command
        data[1] = id
        data[2] = key
        data[3] = -2
        var index = 4
        if (displayInterface.userLogo) {
            data[index++] = 0.toByte()
        }
        if (displayInterface.time) {
            data[index++] = 1.toByte()
        }
        if (displayInterface.steps) {
            data[index++] = 2.toByte()
        }
        if (displayInterface.calories) {
            data[index++] = 3.toByte()
        }
        if (displayInterface.distanceCovered) {
            data[index++] = 4.toByte()
        }
        if (displayInterface.activeTime) {
            data[index++] = 5.toByte()
        }
        if (displayInterface.stepsGoalProgress) {
            data[index++] = 6.toByte()
        }
        if (displayInterface.displayGoalReached) {
            data[index++] = 7.toByte()
        }
        if (displayInterface.alarm) {
            data[index++] = 8.toByte()
        }
        if (displayInterface.sedentary) {
            data[index++] = 9.toByte()
        }
        if (displayInterface.notifications) {
            data[index++] = 10.toByte()
        }
        if (displayInterface.callReminder) {
            data[index++] = 11.toByte()
        }
        if (displayInterface.heartRate) {
            data[index++] = 12.toByte()
        }
        if (displayInterface.reminder) {
            data[index++] = 13.toByte()
        }
        for (i in index until 20) {
            data[i] = -2
        }
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

    fun callback(callback: DataCallback<CoreDisplayInterface>): R2DisplayInterfaceCommand {
        listener = callback
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && (byteArray[1] != id || byteArray[2] != key)) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 4) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parseData(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    private fun parseData(byteArray: ByteArray) {
        when (byteArray[3].asInt()) {
            237 -> {
                listener?.onResult(Response.Status(true))
            }
            254 -> {
                if (byteArray.size == 20) {
                    var userLogo = false
                    var calories = false
                    var distanceCovered = false
                    var activeTime = false
                    var stepsGoalProgress = false
                    var displayGoalReached = false
                    var alarm = false
                    var sedentary = false
                    var notifications = false
                    var callReminder = false
                    var heartRate = false
                    var reminder = false

                    for (i in byteArray) {
                        when (i) {
                            0.toByte() -> {
                                userLogo = true
                            }
                            3.toByte() -> {
                                calories = true
                            }
                            4.toByte() -> {
                                distanceCovered = true
                            }
                            5.toByte() -> {
                                activeTime = true
                            }
                            6.toByte() -> {
                                stepsGoalProgress = true
                            }
                            7.toByte() -> {
                                displayGoalReached = true
                            }
                            8.toByte() -> {
                                alarm = true
                            }
                            9.toByte() -> {
                                sedentary = true
                            }
                            10.toByte() -> {
                                notifications = true
                            }
                            11.toByte() -> {
                                callReminder = true
                            }
                            12.toByte() -> {
                                heartRate = true
                            }
                            13.toByte() -> {
                                reminder = true
                            }
                        }
                    }
                    listener?.onResult(
                        Response.Result(
                            CoreDisplayInterface(
                                userLogo = userLogo,
                                calories = calories,
                                distanceCovered = distanceCovered,
                                activeTime = activeTime,
                                stepsGoalProgress = stepsGoalProgress,
                                displayGoalReached = displayGoalReached,
                                alarm = alarm,
                                sedentary = sedentary,
                                notifications = notifications,
                                callReminder = callReminder,
                                heartRate = heartRate,
                                reminder = reminder
                            )
                        )
                    )
                } else {
                    listener?.onResult(Response.Status(false))
                }
            }
        }
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }
}