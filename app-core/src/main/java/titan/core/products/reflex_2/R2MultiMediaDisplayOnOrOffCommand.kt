package titan.core.products.reflex_2

import android.os.Message
import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*
import kotlin.collections.HashMap

private const val command: Byte = -66
private const val id: Byte = 2
private const val key: Byte = 7
private const val endKey: Byte = -34
const val R2_MULTI_MEDIA_DISPLAY_ON_OR_OFF = "r2_multi_media_display_on_or_off"

internal class R2MultiMediaDisplayOnOrOffCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null

    fun setData(state:Boolean) : R2MultiMediaDisplayOnOrOffCommand{
        var data = byteArrayOf(command, id, key, -2, if (state) 1 else 0, -19)
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

    fun callback(listener: DataCallback<Boolean>): R2MultiMediaDisplayOnOrOffCommand {
        this.listener = listener
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && (byteArray[1] != id || byteArray[2] != key)) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 6) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun parse(byteArray: ByteArray) {
        when (byteArray[3].asInt()) {
            254 -> {
                /*Set Method Req Response will handle here*/
                listener?.onResult(Response.Result(true))
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