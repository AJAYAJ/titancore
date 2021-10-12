package titan.core.products.reflex_3

import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */
private const val commandId: Byte = 3
private const val keyId: Byte = 42

class R3MusicCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null
    fun setData(musicSwitch: Boolean): R3MusicCommand {
        val data: ByteArray = byteArrayOf(
                commandId,
                keyId,
                if (musicSwitch) 170.toByte() else 85
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
            listener?.onResult(Response.Status(false))
            ResponseStatus.INCOMPATIBLE
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun callback(listener: DataCallback<Boolean>): R3MusicCommand {
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

    private fun parse(data: ByteArray) {
        listener?.onResult(Response.Status(true))
    }
}