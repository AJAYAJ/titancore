package titan.core.products.reflex_slay

import titan.core.bluetooth.CommManager
import titan.core.getLocalCalendar
import titan.core.products.*
import titan.core.toBytes
import java.util.*

private const val messageId: Byte = 51
private const val messageLength: Byte = 10
private const val endKey: Byte = 2
private const val endId: Byte = -1
internal class RSUserInfoCommand : DataCommand {
    private var listener: DataCallback<UserInfo>? = null

    fun get(): RSUserInfoCommand {
        val data = byteArrayOf(
            0,
            5
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
        return this
    }

    fun setData(userInfo: UserInfo.RSUserInfo): RSUserInfoCommand {
        val weight = (userInfo.weight * 10)
        val dob: Calendar = getLocalCalendar().apply {
            time = userInfo.dateOfBirth
        }
        val data: ByteArray = byteArrayOf(
            messageLength,
            messageId,
            dob.get(Calendar.YEAR).toBytes()[1],
            dob.get(Calendar.YEAR).toBytes()[0],
            (dob.get(Calendar.MONTH) + 1).toByte(),
            dob.get(Calendar.DATE).toByte(),
            if (userInfo.isMale) 1.toByte() else 0.toByte(),
            weight.toBytes()[1],
            weight.toBytes()[0],
            userInfo.height.toByte(),
            userInfo.maxHR.toByte(),
            userInfo.restingHR.toByte()
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

        return this
    }


    fun callback(callback: DataCallback<UserInfo>): RSUserInfoCommand {
        listener = callback
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
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != endId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 4) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(data = byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun parse(data: ByteArray) {
        listener?.onResult(
            Response.Status(true)
        )
    }
}