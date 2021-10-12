package titan.core.products.reflex_3

import titan.core.*
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */

private const val commandId: Byte = 51
private val keyId: ByteArray = byteArrayOf(8, 0)
private const val version: Byte = 1
private const val operationType: Byte = 2
private val fixedHeader = byteArrayOf(-38, -83, -38, -83)
private val seq:ByteArray = byteArrayOf(0, 0)

class R3WatchDeleteFaceCommand : DataCommand {

    private var listener: DataCallback<WatchFaceInfo>? = null

    fun setData(watchInfo: WatchFaceInfo.R3WatchDeleteWatch): R3WatchDeleteFaceCommand {
        val part1: ByteArray = fixedHeader.plus(version)
        val part2: ByteArray = keyId.plus(seq).plus(operationType).plus(watchInfo.fileName)
        val length:Int = (part1.size + part2.size + 2)/*+2 is for length. Length has 2 bytes.*/
        val data: ByteArray = byteArrayOf(commandId).plus(part1).plus(byteArrayOf(length.toBytes()[1],length.toBytes()[0])).plus(part2)
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
        } else if (byteArray.size < 10 || !byteCopy(
                byteArray,
                1,
                4
            ).contentEquals(fixedHeader) || !byteCopy(byteArray, 8, 2).contentEquals(keyId)
        ) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INCOMPATIBLE
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    private fun parse(byteArray: ByteArray) {
        if (byteArray.size > 13 && (byteArray[12].asInt() == 5 || byteArray[12].asInt() == 0)) {
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

    fun callback(listener: DataCallback<WatchFaceInfo>): R3WatchDeleteFaceCommand {
        this.listener = listener
        return this
    }
}