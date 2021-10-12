package titan.core.products.reflex_3

import titan.core.bluetooth.CommManager
import titan.core.products.*
import titan.core.to32BitByte
import titan.core.toBytes
import java.util.*

/*
*
* To Send data:
030300401F00000000
Position0: Command_Id
Position1: Key_Id
Position2: Type (00) -> step
Position3, 4, 5, 6: Step_target -> 8000 (1F40) &
distance(2 bytes)
Position7: Sleep_hour
Position8: Sleep_minute
Remaining 11 bytes are reserved */

private const val commandId: Byte = 3
private const val keyId: Byte = 3

class R3StepGoalSettings : DataCommand {
    private var listener: DataCallback<UserInfo>? = null

    fun setData(userInfo: UserInfo.R3UserGoalSettings): R3StepGoalSettings {
        val sleepHour = userInfo.sleepTarget/60
        val sleepMin = userInfo.sleepTarget%60
        val stepsTarget = userInfo.stepsTarget.to32BitByte()
        val data = byteArrayOf(
            commandId,
            keyId,
            GoalType.getValue(userInfo.goalType),
            stepsTarget[3],
            stepsTarget[2],
            stepsTarget[1],
            stepsTarget[0],
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
            listener?.onResult(Response.Status(true))
            ResponseStatus.COMPLETED
        }
    }

    fun callback(listener: DataCallback<UserInfo>): R3StepGoalSettings {
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