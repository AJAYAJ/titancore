package titan.core.products.reflex_2

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import titan.core.toInt
import titan.core.toLong
import java.util.*

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

private const val command: Byte = -66
private const val id: Byte = 1
private const val key: Byte = 3
private const val endKey: Byte = -34

internal class R2UserInfoCommand : DataCommand {
    private var listener: DataCallback<UserInfo>? = null

    fun get(): R2UserInfoCommand {
        val data = byteArrayOf(
            command,
            id,
            key,
            -19
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

    fun set(userInfo: UserInfo.R2UserInfo): R2UserInfoCommand {
        val targetSteps = userInfo.stepsTarget
        val height = (userInfo.height * 100)
        val weight = (userInfo.weight * 100)
        val strideLength = userInfo.getStrideLength() * 100

        val data = byteArrayOf(
            command,
            id,
            key,
            -2,
            (height and 0xffff shr 8).toByte(),
            height.toByte(),
            userInfo.age.toByte(),
            if (userInfo.isMale) 1.toByte() else 0.toByte(),
            (weight shr 8).toByte(),
            weight.toByte(),
            (targetSteps shr 16).toByte(),
            (targetSteps shr 8).toByte(),
            targetSteps.toByte(),
            (strideLength shr 8).toByte(),
            strideLength.toByte(),
            (userInfo.sleepTarget / 60).toByte(),
            (userInfo.sleepTarget % 60).toByte()
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

    fun callback(listener: DataCallback<UserInfo>): R2UserInfoCommand {
        this.listener = listener
        return this
    }

    fun check(packet: ByteArray): ResponseStatus {
        return if (packet.isNotEmpty() && packet[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (packet.size > 2 && (packet[1] != id || packet[2] != key)) {
            ResponseStatus.INCOMPATIBLE
        } else if (packet.size < 4) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(packet)
            ResponseStatus.COMPLETED
        }
    }

    fun parse(packet: ByteArray) {
        when (packet[3].asInt()) {
            251 -> {
                listener?.onResult(
                    Response.Result(
                        UserInfo.R2UserInfo(
                            height = byteArrayOf(packet[4], packet[5]).toInt(false),
                            age = packet[6].toInt(),
                            isMale = packet[7].toInt() == 1,
                            weight = byteArrayOf(packet[8], packet[9]).toInt(false),
                            stepsTarget = byteArrayOf(packet[10], packet[11], packet[12]).toLong(
                                false
                            ),
                            sleepTarget = packet[15].toInt() * 60 + packet[16].toInt()
                        )
                    )
                )
            }
            237 -> {
                /*Response type is boolean and Don't change the status type there is a dependency on step goal setings. Change in sleep goal settings page, If you change the response type*/
                listener?.onResult(Response.Status(true))
            }
            else -> {
                /*Response type is boolean and Don't change the status type there is a dependency on step goal setings. Change in sleep goal settings page, If you change the response type*/
                listener?.onResult(Response.Status(false/*,"UNKNOWN_ERROR"*/))
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