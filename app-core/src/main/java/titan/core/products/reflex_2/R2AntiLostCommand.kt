package titan.core.products.reflex_2

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import titan.core.products.DataCommand
import java.util.*

private const val command: Byte = -66
private const val id: Byte = 6
private const val enableAntiLostKey: Byte = 13
private const val disableAntiLostKey: Byte = 14
private const val endKey: Byte = -34

internal class R2AntiLostCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null

    fun setData(isEnable:Boolean): R2AntiLostCommand {
        var data: ByteArray = if (isEnable){
            byteArrayOf(command, id, enableAntiLostKey, -19)
        }else{
            byteArrayOf(command, id, disableAntiLostKey, -19)
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

    fun callback(listener: DataCallback<Boolean>): R2AntiLostCommand {
        this.listener = listener
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && byteArray[1] != id && (byteArray[2] != enableAntiLostKey || byteArray[2] != disableAntiLostKey)) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 4) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun parse(byteArray: ByteArray) {
        when (byteArray[2].asInt()) {
            enableAntiLostKey.asInt() -> {
                listener?.onResult(Response.Status(true))
            }
            disableAntiLostKey.asInt() -> {
                listener?.onResult(Response.Status(true))
            }
            else -> {
                listener?.onResult(Response.Status(false))
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