package titan.core.products.reflex_3

import titan.core.bluetooth.CommManager
import titan.core.products.*
import titan.core.to32BitByte
import java.util.*

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */
private const val commandId: Byte = 3
private const val keyId: Byte = 4

class R3SleepGoalSettings : DataCommand {
    private var listener: DataCallback<UserInfo>? = null

    fun setData(userInfo: UserInfo.R3SleepGoalSettings): R3SleepGoalSettings {
        val sleepHour = userInfo.sleepTarget/60
        val sleepMin = userInfo.sleepTarget%60
        val data = byteArrayOf(
            commandId,
            keyId,
            sleepHour.toByte(),
            sleepMin.toByte()
        )
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = data,
                key = getKey()
            )
        }
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != commandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            ResponseStatus.INCOMPATIBLE
        } else {
            /*Don't change the status type there is a dependency on sleep goal setings. Change in sleep goal settings page, If you change the response type*/
            listener?.onResult(Response.Status(true))
            ResponseStatus.COMPLETED
        }
    }

    fun callback(listener: DataCallback<UserInfo>): R3SleepGoalSettings {
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
}