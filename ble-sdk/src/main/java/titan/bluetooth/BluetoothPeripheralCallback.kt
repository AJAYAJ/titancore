package titan.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import java.util.*

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */
interface BluetoothPeripheralCallback {
    /**
     * Callback invoked when the list of remote services, characteristics and descriptors
     * for the remote device have been updated, ie new services have been discovered.
     */
    fun onServicesDiscovered(services: List<BluetoothGattService>): Boolean

    /**
     * Callback invoked when the notification state of a characteristic has changed.
     *
     *
     * Use [BluetoothPeripheral.isNotifying] to get the current notification state of the characteristic
     *
     * @param characteristic the characteristic for which the notification state changed
     * @param status         GATT status code
     */
    fun onNotificationStateUpdate(
        characteristic: BluetoothGattCharacteristic,
        status: Int
    )

    /**
     * Callback invoked as the result of a characteristic read operation or notification
     *
     *
     * The value byte array is a threadsafe copy of the byte array contained in the characteristic.
     *
     * @param value          the new value received
     * @param characteristic the characteristic for which the new value was received
     * @param status         GATT status code
     * @param notified       Is data came as notification
     *
     * @return Boolean return true if data is received fully
     */
    fun onCharacteristicUpdate(
        value: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        status: Int,
        notified: Boolean,
        key: UUID?
    ): CommandResponseState

    /**
     * Callback indicating the result of a characteristic write operation.
     *
     *
     * The value byte array is a threadsafe copy of the byte array contained in the characteristic.
     *
     * @param value          the value to be written
     * @param characteristic the characteristic written to
     * @param status         GATT status code
     */
    fun onCharacteristicWrite(
        value: ByteArray?,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    )

    /**
     * Callback invoked as the result of a descriptor read operation
     *
     * @param value      the read value
     * @param descriptor the descriptor that was read
     * @param status     GATT status code
     */
    fun onDescriptorRead(
        value: ByteArray,
        descriptor: BluetoothGattDescriptor,
        status: Int
    ) {
    }

    /**
     * Callback invoked as the result of a descriptor write operation.
     * This callback is not called for the Client Characteristic Configuration descriptor. Instead the [BluetoothPeripheralCallback.onNotificationStateUpdate] will be called
     *
     * @param value      the value that to be written
     * @param descriptor the descriptor written to
     * @param status     the GATT status code
     */
    fun onDescriptorWrite(
        value: ByteArray?,
        descriptor: BluetoothGattDescriptor,
        status: Int
    )

    fun isNotification(characteristic: BluetoothGattCharacteristic): Boolean

    fun removeCommand(key: UUID?, isFailed: Boolean = false)

    /**
     * Callback invoked when a bonding process is started
     *
     */
    fun onBondingStarted()

    /**
     * Callback invoked when a bonding process has succeeded
     *
     */
    fun onBondingSucceeded()

    /**
     * Callback invoked when a bonding process has failed
     *
     */
    fun onBondingFailed()

    /**
     * Callback invoked when a bond has been lost and the peripheral is not bonded anymore.
     *
     */
    fun onBondLost()

    /**
     * Callback invoked as the result of a read RSSI operation
     *
     * @param rssi       the RSSI value
     * @param status     GATT status code
     */
    fun onReadRemoteRssi(rssi: Int, status: Int)

    /**
     * Callback invoked as the result of a MTU request operation
     *
     * @param mtu        the new MTU
     * @param status     GATT status code
     */
    fun onMtuChanged(mtu: Int, status: Int)
}