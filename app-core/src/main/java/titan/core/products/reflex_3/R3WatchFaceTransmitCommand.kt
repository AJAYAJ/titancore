package titan.core.products.reflex_3

import titan.core.bluetooth.CommManager
import titan.core.byteMerge
import titan.core.products.*
import java.util.*

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */
private const val commandId: Byte = -47
private const val keyId: Byte = 2

class R3WatchFaceTransmitCommand : DataCommand {
    private var listener: DataCallback<WatchFaceInfo>? = null

    fun setData(watchFaceInfo: WatchFaceInfo.R3TransferWatch): R3WatchFaceTransmitCommand {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = false,
                data = byteMerge(
                    byteArrayOf(
                        commandId,
                        keyId, 0,
                    ), watchFaceInfo.fileStream
                ),
                key = null
            )
        }
        return this
    }

    fun callback(callback: DataCallback<WatchFaceInfo>): R3WatchFaceTransmitCommand {
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
        } else if (byteArray.size < 3) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            ResponseStatus.COMPLETED
        }
    }

    fun checkPNRCommand(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != commandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 3) {
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            ResponseStatus.COMPLETED
        }
    }

    fun getCheckCode(byteArray: ByteArray) : ByteArray {
        return if (byteArray.isNotEmpty() && byteArray[0] != commandId) {
            byteArrayOf()
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            byteArrayOf()
        } else if (byteArray.size < 11) {
            byteArrayOf()
        } else {
            byteArrayOf(byteArray[3],byteArray[4],byteArray[5],byteArray[6])
        }
    }
}