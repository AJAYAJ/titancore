package titan.core.products.reflex_slay

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

private const val messageLength: Byte = 4
private const val messageId: Byte = 0
private const val endKey: Byte = 2
private const val endId: Byte = -1


class RSAskPairCommand : DataCommand {
    private var listener: DataCallback<CoreBinding>? = null

    fun pair(): RSAskPairCommand {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    RS_COMMUNICATION_SERVICE,
                    RS_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(messageLength, messageId, -95, -2, 116, 105),
                key = getKey()
            )
        }
        return this
    }

    fun callback(callback: DataCallback<CoreBinding>): RSAskPairCommand {
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
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != endId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 4) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    private fun parse(data: ByteArray) {
        /**
         * 0 - not paired
         * 1 - paired
         **/
        data.let {
            when (data[3].asInt()) {
                0 -> {
                    listener?.onResult(Response.Status(false))
                }
                1 -> {
                    listener?.onResult(
                        Response.Result(
                            CoreBinding.RSBinding(
                                pairingStatus = true,
                                isBindingCodeRequired = false
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

}