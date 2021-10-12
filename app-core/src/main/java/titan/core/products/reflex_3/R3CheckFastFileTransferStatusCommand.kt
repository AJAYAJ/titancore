package titan.core.products.reflex_3

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */
private const val commandId: Byte = 3
private const val keyId: Byte = 53

class R3CheckFastFileTransferStatusCommand : DataCommand {
    private var listener: DataCallback<WatchFaceInfo>? = null

    fun setData(watchFaceInfo: WatchFaceInfo.R3FileTransferCheckMode): R3CheckFastFileTransferStatusCommand {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                    write = Pair(
                            R3_COMMUNICATION_SERVICE,
                            R3_COMMUNICATION_WRITE_CHAR
                    ),
                    read = null,
                    responseWillNotify = true,
                    data = byteArrayOf(
                            commandId,
                            keyId,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0
                    ),
                    key = getKey()
            )
        }
        return this
    }

    fun callback(callback: DataCallback<WatchFaceInfo>): R3CheckFastFileTransferStatusCommand {
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
        } else if (byteArray.size < 4) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun parse(byteArray: ByteArray) {
        when {
            byteArray[2].asInt() == 1 -> {
                listener?.onResult(
                        Response.Result(
                                WatchFaceInfo.R3FileTransferCheckMode(
                                        bandCurrentState = BandTransferState.FAST,
                                        isCheckModeOn = true
                                )
                        )
                )
            }
            byteArray[2].asInt() == 2 -> {
                listener?.onResult(
                        Response.Result(
                                WatchFaceInfo.R3FileTransferCheckMode(
                                        bandCurrentState = BandTransferState.SLOW,
                                        isCheckModeOn = true
                                )
                        )
                )
            }
            else -> {
                listener?.onResult(Response.Status(false))
            }
        }
    }
}