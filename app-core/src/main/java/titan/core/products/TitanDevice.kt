package titan.core.products

import android.bluetooth.BluetoothGattCharacteristic
import titan.bluetooth.CommandResponseState
import java.util.*

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

internal interface TitanDevice {
    fun dataReceived(
        value: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        status: Int,
        notified: Boolean,
        key: UUID?
    ): CommandResponseState

    fun failed(key: UUID?)
}

data class ResponsePackage(
    val command: String,
    val data: HashMap<Int, ByteArray>
)

@Suppress("ArrayInDataClass")
data class TaskPackage(
    val write: Pair<String, String>?,
    val read: Pair<String, String>?,
    val responseWillNotify: Boolean = false,
    val data: ByteArray?,
    val key: UUID?
) {
    fun getWriteData(): ByteArray {
        data?.let {
            return it
        }
        return byteArrayOf()
    }

    fun getWriteIdentifier(): String {
        return "${write?.first}|${write?.second}"
    }

    fun getReadIdentifier(): String {
        return "${read?.first}|${read?.second}"
    }

    fun performRead(): Boolean {
        return read != null
    }

    fun performWrite(): Boolean {
        return write != null
    }
}