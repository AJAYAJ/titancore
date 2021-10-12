package titan.core.products.reflex_3

import titan.core.bluetooth.CommManager
import titan.core.products.*
import titan.core.toBytes
import java.util.*

/*    0310AA701701C6070101
    Position0: Command_Id
    Position1: Key_Id
    Position2: Height (170) -> AA
    Position3 & 4: Weight (6000) -> 1770 ( 100 times of actual value)
    Position5: Gender (01) -> female
    Position6 & 7: birth_year (1990) -> 07C6
    Position8: birth_month (01)
    Position9: birth_day (01)*/

private const val commandId: Byte = 3
private const val keyId: Byte = 16

class R3UserInfoCommand : DataCommand {
    private var listener: DataCallback<UserInfo>? = null
    fun setData(userInfo: UserInfo.R3UserInfo): R3UserInfoCommand {
        val weight = (userInfo.weight * 100)
        val dob: Calendar = Calendar.getInstance().apply {
            time = userInfo.dateOfBirth
        }
        val data: ByteArray = byteArrayOf(
            commandId,
            keyId,
            userInfo.height.toByte(),
            weight.toBytes()[1],
            weight.toBytes()[0],
            if (userInfo.isMale) 1.toByte() else 0.toByte(),
            dob.get(Calendar.YEAR).toBytes()[1],
            dob.get(Calendar.YEAR).toBytes()[0],
            (dob.get(Calendar.MONTH) + 1).toByte(),
            dob.get(Calendar.DATE).toByte()
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

    fun callback(callback: DataCallback<UserInfo>): R3UserInfoCommand {
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
        return if (byteArray.isNotEmpty() && byteArray[0] != commandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            ResponseStatus.INCOMPATIBLE
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
