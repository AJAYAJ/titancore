package titan.core.products.reflex_3

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */

private const val commandId: Byte = 6
private const val keyId: Byte = 2

private const val notifyEventCommandId: Byte = 7
private const val notifyStartCameraEventKeyId: Byte = 64
private const val notifyStopCameraEventKeyId: Byte = 1
private val cameraActionCommands = byteArrayOf(6, 7, 11)

class R3CameraCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null
    fun setData(mode: CameraMode): R3CameraCommand {
        val data: ByteArray = byteArrayOf(
            commandId,
            keyId,
            if (mode == CameraMode.START_CAMERA) 0 else 1
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

    private fun sendOpenAck(): R3CameraCommand {
        val data: ByteArray = byteArrayOf(
            notifyEventCommandId,
            notifyStartCameraEventKeyId,
            0.toByte()
        )
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = false,
                data = data,
                key = getKey()
            )
        }
        return this
    }

    private fun sendCloseAck(): R3CameraCommand {
        val data: ByteArray = byteArrayOf(
            notifyEventCommandId,
            notifyStopCameraEventKeyId,
            0.toByte()
        )
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = false,
                data = data,
                key = getKey()
            )
        }
        return this
    }

    private fun sendCaptureAck(): R3CameraCommand {
        val data: ByteArray = byteArrayOf(
            notifyEventCommandId,
            notifyStopCameraEventKeyId,
            0.toByte()
        )
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = false,
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
        } else if (byteArray.size < 3) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun checkEventControl(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != notifyEventCommandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && cameraActionCommands.contains(byteArray[2])) {
            if (byteArray[1] == notifyStartCameraEventKeyId && byteArray[2] == 7.toByte()) {
                sendOpenAck()
                ResponseStatus.COMPLETED
            } else if (byteArray[1] == notifyStopCameraEventKeyId && byteArray[2] == 11.toByte()) {
                sendCloseAck()
                ResponseStatus.COMPLETED
            } else if (byteArray[1] == notifyStopCameraEventKeyId && byteArray[2] == 6.toByte()) {
                sendCaptureAck()
                ResponseStatus.COMPLETED
            } else {
                ResponseStatus.INCOMPATIBLE
            }
        } else {
            ResponseStatus.INCOMPATIBLE
        }
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    fun callback(listener: DataCallback<Boolean>): R3CameraCommand {
        this.listener = listener
        return this
    }


    override fun failed() {
        listener?.onResult(Response.Status(false))
    }

    fun parse(data: ByteArray) {
        listener?.onResult(Response.Status(data[2].asInt() == 0))
    }
}