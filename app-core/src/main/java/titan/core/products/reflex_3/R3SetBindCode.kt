package titan.core.products.reflex_3

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */

private const val commandId: Byte = 4
private const val keyId: Byte = 3

class R3SetBindCode:DataCommand{
    private var listener: DataCallback<BindingStatus>? = null

    fun setData(code:String): R3SetBindCode {
        var data: ByteArray = byteArrayOf(
            commandId,
            keyId,2,2,0
        )
        data = data.plus(code.length.toByte())
        code.forEach {
            data = data.plus(it.toString().toInt().toByte())
        }
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

    fun callback(callback: DataCallback<BindingStatus>): R3SetBindCode {
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
        } else if (byteArray.size<3){
            listener?.onResult(
                Response.Status(false)
            )
            ResponseStatus.INVALID_DATA_LENGTH
        }else{
            parse(data = byteArray)
            ResponseStatus.COMPLETED
        }
    }

    /*status（0x00:Successful ，0x01：failed, 0x02:Loss of binding code failed）*/
    fun parse(data: ByteArray) {
        when(data[2].asInt()){
            0->{
                listener?.onResult(
                    Response.Result(BindingStatus.BOUNDED)
                )
            }
            1 ->{
                listener?.onResult(
                    Response.Result(BindingStatus.UN_BOUNDED)
                )
            }
            2 ->{
                listener?.onResult(
                    Response.Result(BindingStatus.RE_BOUND_REQUIRED)
                )
            }
            else->{
                listener?.onResult(
                    Response.Status(false)
                )
            }
        }
    }
}