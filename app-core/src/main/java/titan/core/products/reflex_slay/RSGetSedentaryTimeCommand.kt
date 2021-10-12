package titan.core.products.reflex_slay

import com.google.gson.Gson
import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

private const val endKey: Byte = 6
private const val endId: Byte = -1
internal class RSGetSedentaryTimeCommand : DataCommand{
    private var listener: DataCallback<SedentaryReminder>? = null

    fun get(): RSGetSedentaryTimeCommand {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    RS_COMMUNICATION_SERVICE,
                    RS_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(0, 3),
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

    private fun parse(byteArray: ByteArray) {
        byteArray.let {
            val data: ArrayList<Sedentary> = arrayListOf()
            data.add(
                Sedentary(
                    start = Pair(byteArray[4].asInt(), byteArray[5].asInt()),
                    end = Pair(byteArray[6].asInt(), byteArray[7].asInt()),
                    interval = 30,
                    isSedentaryOn = true
                )
            )
            val reminder = SedentaryReminder(
                list = data,
                restTime = byteArray[3].asInt() ,
                isSedentaryOn = !data.isNullOrEmpty()
            )
            println(Gson().toJson(reminder).toString())
            listener?.onResult(
                Response.Result(
                    reminder
                )
            )
        }
    }

    fun callback(listener: DataCallback<SedentaryReminder>): RSGetSedentaryTimeCommand {
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