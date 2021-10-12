package titan.core.products.reflex_3

import android.os.Message
import titan.core.asHex
import titan.core.bluetooth.CommManager
import titan.core.products.BaseParser
import titan.core.products.ResponsePackage
import titan.core.products.TaskPackage
import titan.core.products.ResponseStatus

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

private const val commandId: Byte = 2
private const val keyId: Byte = 4
const val R3_MAC_ADDRESS = "r3_mac_address"

internal object R3MacAddressCommand {
    fun getData() {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(commandId, keyId),
                key = null
            )
        }
    }

    private var parseData = HashMap<Int, ByteArray>()

    fun getMessage(): Message {
        val message = Message()
        message.what = 1
        message.obj = ResponsePackage(R3_MAC_ADDRESS, HashMap(parseData))
        parseData.clear()
        return message
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != commandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 8) {
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parseData[0] = byteArray
            ResponseStatus.COMPLETED
        }
    }
}

internal data class R3MacAddress(val macAddress: String) {

    class Parser : BaseParser {
        private lateinit var model: R3MacAddress

        override fun parse(data: HashMap<Int, ByteArray>) {
            data[0]?.let { byteArray ->
                model =
                    R3MacAddress(macAddress = "${byteArray[2].asHex()}:${byteArray[3].asHex()}:${byteArray[4].asHex()}:${byteArray[5].asHex()}:${byteArray[6].asHex()}:${byteArray[7].asHex()}")
//                SharedPreference.macAddress = model.macAddress
                println(model.macAddress)
            }
//            if (!::model.isInitialized || model.macAddress != SharedPreference.macAddress) {
//                CommManager.getInstance().disconnect(model.macAddress)
//            } else {
//                CommManager.getInstance().wearable?.saveIdentifier()
//                R3Manager.getBasicInfo()
//            }
        }
    }
}