package titan.core.products.reflex_3

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import titan.core.to32BitByte
import java.util.*

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */

private const val commandId: Byte = -47
private const val keyId: Byte = 3

class R3WatchEndTransferCommand : DataCommand {
    private var listener: DataCallback<WatchFaceInfo>? = null

    fun setData(watchFaceInfo: WatchFaceInfo.R3EndTransfer): R3WatchEndTransferCommand {
        val checkSum = watchFaceInfo.checkSum.to32BitByte()
        val data: ByteArray = byteArrayOf(
                commandId,
                keyId,
                checkSum[3],
                checkSum[2],
                checkSum[1],
                checkSum[0]
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

    fun callback(callback: DataCallback<WatchFaceInfo>): R3WatchEndTransferCommand {
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
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun parse(byteArray: ByteArray) {
        if (byteArray[2].asInt() == 0) {
            listener?.onResult(Response.Status(true))
        } else {
            listener?.onResult(Response.Status(false))
        }
    }

    fun checkEventCommand(byteArray: ByteArray): ResponseStatus {
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

    fun getEventCommand(byteArray: ByteArray): WatchFaceTransferState {
        return when {
            byteArray[2].asInt() == 13 -> {
                WatchFaceTransferState.OPERATION_TIMED_OUT
            }
            byteArray[2].asInt() == 0 -> {
                WatchFaceTransferState.COMPLETED
            }
            else -> {
                WatchFaceTransferState.NONE
            }
        }
    }
}