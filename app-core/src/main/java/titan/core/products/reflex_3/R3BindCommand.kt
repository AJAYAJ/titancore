package titan.core.products.reflex_3

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

private const val commandId: Byte = 4
private const val keyId: Byte = 1

class R3BindCommand : DataCommand {
    private var listener: DataCallback<CoreBinding>? = null

    fun bind(): R3BindCommand {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(commandId, keyId, -15, 1, 1, 2, 2, 0),
                key = getKey()
            )
        }
        return this
    }

    fun callback(callback: DataCallback<CoreBinding>): R3BindCommand {
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
         * 0 means Successful
         * 1 means failed
         * 2 means already bound
         **/
        data.let {
            when (data[2].asInt()) {
                1 -> {
                    listener?.onResult(Response.Status(false))
                }
                0 -> {
                    /**
                     * Binding Code Requires, if byte 3 is not 0
                     **/
                    if (data[3].asInt() == 0) {
                        listener?.onResult(
                            Response.Result(
                                CoreBinding.R3Binding(
                                    bindingStatus = true,
                                    isBindingCodeRequired = false,
                                    bindingCodeLength = data[3].asInt()
                                )
                            )
                        )
                    } else {
                        listener?.onResult(
                            Response.Result(
                                CoreBinding.R3Binding(
                                    bindingStatus = true,
                                    isBindingCodeRequired = true,
                                    bindingCodeLength = data[3].asInt()
                                )
                            )
                        )
                    }
                }
                2 -> {
                    listener?.onResult(
                        Response.Result(
                            CoreBinding.R3Binding(
                                bindingStatus = true,
                                isBindingCodeRequired = false,
                                bindingCodeLength = data[3].asInt()
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