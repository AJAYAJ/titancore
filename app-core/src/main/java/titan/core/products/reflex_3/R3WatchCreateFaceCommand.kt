package titan.core.products.reflex_3

import titan.core.*
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */

private const val commandId: Byte = -47
private const val keyId: Byte = 1
private const val fileType: Byte = -1
private const val compressionType: Byte = 2

class R3WatchCreateFaceCommand : DataCommand {
    private var listener: DataCallback<WatchFaceInfo>? = null

    fun setWatchFace(watchFaceInfo: WatchFaceInfo.R3CreateWatch): R3WatchCreateFaceCommand {
        val fileNameByteArray: ByteArray = if (watchFaceInfo.fileName.length > 12) {
            watchFaceInfo.fileName.substring(0, 12).toByteArray()
        } else {
            val empty = ByteArray(12)
            val b = watchFaceInfo.fileName.toByteArray()
            for (i in b.indices) {
                empty[i] = b[i]
            }
            empty
        }
        val sizeArray = watchFaceInfo.fileSize.to32BitByte()
        val data: ByteArray = byteMerge(
            byteArrayOf(
                commandId,
                keyId,
                fileType,
                sizeArray[3],
                sizeArray[2],
                sizeArray[1],
                sizeArray[0],
                compressionType,
            ),
            fileNameByteArray,
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
        return if (byteArray.isEmpty()) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray[0] != commandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 3) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            return ResponseStatus.COMPLETED
        }
    }

    private fun parse(byteArray: ByteArray) {
        if (byteArray[2].asInt() == 0) {
            listener?.onResult(Response.Status(true))
        } else {
            listener?.onResult(Response.Status(false))
        }
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }

    fun callback(listener: DataCallback<WatchFaceInfo>): R3WatchCreateFaceCommand {
        this.listener = listener
        return this
    }
}