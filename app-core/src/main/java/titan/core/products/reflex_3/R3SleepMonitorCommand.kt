package titan.core.products.reflex_3

import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*


/*
03 31 AA 11 28 11 2D
Position0: Command_Id
Position1: Key_Id
Position2: switch(AA) -> on
Position 3:start_hour
Position 4:start_minute
Position 5:end_hour
Position 6:end_minute
Remaining 13 bytes are reserved
*/

private const val commandId: Byte = 3
private const val keyId: Byte = 49

class R3SleepMonitorCommand : DataCommand{
    private var listener: DataCallback<AutoSleep>? = null
    fun set(autoSleep: AutoSleep) : R3SleepMonitorCommand{
        val data: ByteArray = byteArrayOf(
            commandId,
            keyId,
            if (autoSleep.autoSleep) -86 else 85,
            autoSleep.sleepStart.first.toByte(),
            autoSleep.sleepStart.second.toByte(),
            autoSleep.sleepEnd.first.toByte(),
            autoSleep.sleepEnd.second.toByte()
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

    fun callback(listener: DataCallback<AutoSleep>): R3SleepMonitorCommand {
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