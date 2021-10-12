package titan.core.products.reflex_3

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */

private const val commandId: Byte = 1
private const val keyId: Byte = 1

class R3EnterOTAMode : DataCommand {
    private var listener: DataCallback<R3OTAStatus>? = null
    fun setData(): R3EnterOTAMode {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(
                    commandId,
                    keyId
                ),
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
            ResponseStatus.COMPLETED
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun callback(listener: DataCallback<R3OTAStatus>): R3EnterOTAMode {
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

    fun parse(data: ByteArray) {
        when {
            data[2].asInt() == 0 -> {
                listener?.onResult(
                    Response.Result(
                        R3OTAStatus(
                            state = OTAState.DFU_MODE,
                            status = true
                        )
                    )
                )
            }
            data[2].asInt() == 1 -> {
                listener?.onResult(
                    Response.Result(
                        R3OTAStatus(
                            state = OTAState.BATTERY_LOW,
                            status = false
                        )
                    )
                )
            }
            data[2].asInt() == 2 -> {
                listener?.onResult(
                    Response.Result(
                        R3OTAStatus(
                            state = OTAState.NOT_SUPPORTED,
                            status = false
                        )
                    )
                )
            }
            data[2].asInt() == 3 -> {
                listener?.onResult(
                    Response.Result(
                        R3OTAStatus(
                            state = OTAState.FAILED,
                            status = false
                        )
                    )
                )
            }
            else -> {
                listener?.onResult(
                    Response.Result(
                        R3OTAStatus(
                            state = OTAState.NONE,
                            status = true
                        )
                    )
                )
            }
        }
    }
}

data class R3OTAStatus(
    val state: OTAState,
    val status: Boolean
)

enum class OTAState {
    NONE,
    FAILED,
    NOT_SUPPORTED,
    BATTERY_LOW,
    DFU_MODE,
    ;
}