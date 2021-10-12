package titan.core.products.reflex_2

import android.content.Context
import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.bluetooth.ReflexProducts
import titan.core.products.*
import java.util.*

private const val command: Byte = -66
private const val id: Byte = 1
private const val key: Byte = 11
private const val endKey: Byte = -34


internal class R2WristSelectionCommand : DataCommand {
    private var listener: DataCallback<WristSelection>? = null

    fun setData(product: String, wristSelection: WristSelection): R2WristSelectionCommand {
        var data = byteArrayOf(
            command,
            id,
            key,
            -2
        )
        when (product){
            ReflexProducts.REFLEX_BEAT.name->{
                data = data.plus(if (wristSelection.isLeftHand) 0.toByte() else 1.toByte())
            }
            else -> {
                data = data.plus(if (wristSelection.isLeftHand) 1.toByte() else 0.toByte())
            }
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

    fun callback(listener: DataCallback<WristSelection>): R2WristSelectionCommand {
        this.listener = listener
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && (byteArray[1] != id || byteArray[2] != key)) {
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
        when (byteArray[3].asInt()) {
            237 -> {
                /*Set Method Req Response will handle here*/
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