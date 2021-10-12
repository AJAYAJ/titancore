package titan.core.bluetooth


/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

interface ScanListener {
    fun onDeviceFound(
        displayName: String,
        productName: String,
        address: String,
        productCode: String,
        productGroup: String,
        rssi: Int
    )

    fun updateRSSI(address: String, rssi: Int)
    fun scanStopped()
    fun scanStarted()
}

fun CommManager.subscribe(listener: ScanListener) {
    scanListeners.add(listener)
}

fun CommManager.unsubscribe(listener: ScanListener) {
    scanListeners.remove(listener)
}

interface BluetoothStateListener {
    fun on()
    fun off()
}

fun CommManager.subscribe(listener: BluetoothStateListener) {
    bluetoothStateListeners.add(listener)
}

fun CommManager.unsubscribe(listener: BluetoothStateListener) {
    bluetoothStateListeners.remove(listener)
}

interface DeviceConnectionListener {
    fun connected()
    fun connecting()
    fun disconnecting()
    fun connectionFailed(status: Int)
    fun disconnected(status: Int)
}