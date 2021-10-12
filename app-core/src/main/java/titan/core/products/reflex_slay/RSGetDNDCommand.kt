package titan.core.products.reflex_slay

import com.google.gson.Gson
import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

private const val endKey: Byte = 6
private const val endId: Byte = -1

internal class RSGetDNDCommand : DataCommand {
    private var listener: DataCallback<CoreDND>? = null

    fun get(): RSGetDNDCommand {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    RS_COMMUNICATION_SERVICE,
                    RS_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(0, 6),
                key = getKey()
            )
        }
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != endId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 8) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    private fun parse(data: ByteArray) {
        data.let {
            val model = CoreDND(
                isOn =  data[3].asInt() == 1,
                start = Pair(data[4].asInt(), data[5].asInt()),
                end = Pair(data[6].asInt(), data[7].asInt())
            )
            println(Gson().toJson(model).toString())
            listener?.onResult(
                Response.Result(
                    model
                )
            )
        }
    }

    fun callback(listener: DataCallback<CoreDND>): RSGetDNDCommand {
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
}