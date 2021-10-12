package titan.core.products.reflex_3

import android.os.Message
import com.google.gson.Gson
import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

private const val commandId: Byte = 8
private const val keyId: Byte = 1

class R3StartSyncCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null

    fun startManualSync() :R3StartSyncCommand{
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_HEALTH_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(commandId, keyId, 1, 0),
                key = getKey()
            )
        }
        return this
    }

    fun startAutoSync() :R3StartSyncCommand{
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_HEALTH_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(commandId, keyId, 2, 0),
                key = getKey()
            )
        }
        return this
    }

    fun callback(callback: DataCallback<Boolean>): R3StartSyncCommand {
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
        } else if (byteArray.size > 1 && (byteArray[1] != keyId)) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 8) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            listener?.onResult(Response.Result(true))
            ResponseStatus.COMPLETED
        }
    }

    fun parse(data: ByteArray) {
        data?.let {
            /*it[1] == 1.toByte() -> {
            model = R3Sync(
                steps = byteArray[4].asInt(),
                sleep = byteArray[5].asInt(),
                hr = byteArray[6].asInt()
            )
            println(Gson().toJson(model))
            model.syncPreviousDaysData()*/
        }
    }
}