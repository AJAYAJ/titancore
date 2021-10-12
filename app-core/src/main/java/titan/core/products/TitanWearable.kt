package titan.core.products

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.google.gson.JsonParseException
import titan.bluetooth.*
import titan.core.ACTION_BAND_STATE
import titan.core.ACTION_DEVICE_CONNECTION_STATE
import titan.core.EXTRA_BAND_STATE
import titan.core.EXTRA_DEVICE_CONNECTION_STATE
import titan.core.bluetooth.DeviceConfig
import titan.core.bluetooth.Options
import titan.core.bluetooth.ReflexProducts
import titan.core.products.reflex_2.Reflex2
import titan.core.products.reflex_3.Reflex3
import titan.core.products.reflex_slay.ReflexSlay
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

internal class TitanWearable constructor(
    private val context: Context,
    private val bluetoothPeripheral: BluetoothPeripheral
) {
    private val handler = Handler(Looper.getMainLooper())
    val address: String = bluetoothPeripheral.address
    val productInfo by lazy { bluetoothPeripheral.productInfo }
    private var bandState: BandState = BandState.NONE
    private var isOnlyDFUServiceAvailable: Boolean = false
    private var isRequiredServicesAvailable: Boolean = false

    fun isConnected(): Boolean {
        return bluetoothPeripheral.getState() == ConnectionState.STATE_CONNECTED
    }

    fun getBandState(): BandState = bandState

    internal val device by lazy {
        when {
            ReflexProducts.isReflexSlay(productInfo?.getCode()) -> ReflexSlay(
                context
            )
            ReflexProducts.isReflex3(productInfo?.getCode()) -> Reflex3(
                context
            )
            ReflexProducts.isReflex2(productInfo?.getCode()) -> Reflex2(
                context
            )
            else -> throw Exception("${productInfo?.getCode()} is not supported")
        }
    }

    private val localCharacteristics = HashMap<String, BluetoothGattCharacteristic>()
    private val localDescriptor = HashMap<String, BluetoothGattDescriptor>()

    private val config: DeviceConfig by lazy {
        try {
            val open = context.assets.open(getFile())
            val bArr = ByteArray(open.available())
            open.read(bArr)
            open.close()
            Gson().fromJson(
                String(bArr),
                DeviceConfig::class.java
            )
        } catch (e: IOException) {
            throw IOException("Unable to load products json.")
        } catch (e: JsonParseException) {
            throw JsonParseException("Unable to configure products.")
        }
    }

    val settings = config.settings
    val aboutBand = config.aboutBand

    private fun getFile(): String {
        return when (ReflexProducts.getProduct(productInfo?.getCode())) {
            ReflexProducts.REFLEX_3 -> "reflex_3.json"
            ReflexProducts.REFLEX_SLAY -> "reflex_slay.json"
            ReflexProducts.REFLEX_2C -> "reflex_2c.json"
            else -> throw Exception("${productInfo?.getCode()} is not supported.")
        }
    }

    fun getProductOptions(): Options {
        return config.options
    }

    fun getDFUService(): List<String> {
        println(Gson().toJson(config.dfuService))
        return config.dfuService
    }

    fun isDFUSupported(): Boolean {
        return config.dfu
    }

    fun isOnlyDFUServiceAvailable(): Boolean {
        return isOnlyDFUServiceAvailable
    }

    fun isRequiredServicesAvailable(): Boolean {
        return isRequiredServicesAvailable
    }

    fun getBandStateCallback(): BandStateCallback {
        return bandStateCallback
    }

    fun getConnectionCallback(): DeviceConnectionCallback {
        return connectionCallback
    }

    private val bandStateCallback = object : BandStateCallback {
        override fun none() {
            bandState = BandState.NONE
            postBandState(BandState.NONE)
        }

        override fun ready() {
            if (bandState < BandState.READY) {
                bandState = BandState.READY
                postBandState(BandState.READY)
            }
        }

        override fun checkingFOTA() {
            bandState = BandState.FOTA_UPDATE
            postBandState(BandState.FOTA_UPDATE)
        }

        override fun updatedFOTA() {
            bandState = BandState.AVAILABLE
            postBandState(BandState.AVAILABLE)
        }

        override fun dataSyncStart() {
            bandState = BandState.DATA_SYNC
            postBandState(BandState.DATA_SYNC)
        }

        override fun available() {
            bandState = BandState.AVAILABLE
            postBandState(BandState.AVAILABLE)
        }
    }

    private val connectionCallback = object : DeviceConnectionCallback {
        override fun connecting() {
            bandState = BandState.NONE
            postConnectionState(ConnectionState.STATE_CONNECTING)
        }

        override fun connected() {
//            requestMTU()
//            startRSSITimer()
            postConnectionState(ConnectionState.STATE_CONNECTED)
        }

        override fun failed() {
            bandState = BandState.NONE
            postConnectionState(ConnectionState.STATE_FAILED)
        }

        override fun disconnected() {
            bandState = BandState.NONE
//            stopRSSITimer()
            postConnectionState(ConnectionState.STATE_DISCONNECTED)
        }

        override fun disconnecting() {
            postConnectionState(ConnectionState.STATE_DISCONNECTING)
        }
    }

    fun postConnectionState(state: ConnectionState) {
        val intent = Intent(ACTION_DEVICE_CONNECTION_STATE)
        intent.putExtra(EXTRA_DEVICE_CONNECTION_STATE, state.value)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun postBandState(state: BandState) {
        val intent = Intent(ACTION_BAND_STATE)
        intent.putExtra(EXTRA_BAND_STATE, state.value)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun startRSSITimer() {
        handler.postDelayed(rssiRunnable, 60000)
    }

    private fun stopRSSITimer() {
        handler.removeCallbacks(rssiRunnable)
    }

    private val rssiRunnable = Runnable {
        readRSSI()
        startRSSITimer()
    }

    private val bluetoothPeripheralCallback = object : BluetoothPeripheralCallback {
        override fun onServicesDiscovered(services: List<BluetoothGattService>): Boolean {
            val requiredServices = config.getRequiredServices()
            isOnlyDFUServiceAvailable =
                checkIsOnlyDFUServiceAvailbleORNot(services, requiredServices, config)
            isRequiredServicesAvailable =
                checkIsRequiredServicesAvailableOrNot(services, requiredServices)
            services.forEach {
                requiredServices.remove(it.uuid.toString())
                for (characteristic in it.characteristics) {
                    localCharacteristics["${it.uuid}|${characteristic.uuid}".toLowerCase(Locale.getDefault())] =
                        characteristic
                }
            }
            return requiredServices.isEmpty()
        }

        override fun onNotificationStateUpdate(
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {

        }

        override fun onCharacteristicUpdate(
            value: ByteArray,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
            notified: Boolean,
            key: UUID?
        ): CommandResponseState {
            return device.dataReceived(
                value,
                characteristic,
                status,
                notified,
                key
            )
        }

        override fun onCharacteristicWrite(
            value: ByteArray?,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {

        }

        override fun onDescriptorWrite(
            value: ByteArray?,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {

        }

        override fun isNotification(characteristic: BluetoothGattCharacteristic): Boolean {
            return config.isNotification(characteristic)
        }

        override fun removeCommand(key: UUID?, isFailed: Boolean) {
            key?.let {
                if (isFailed) {
                    device.failed(it)
                }
                Commands.getInstance().remove(it)
            }
        }

        override fun onBondingStarted() {

        }

        override fun onBondingSucceeded() {

        }

        override fun onBondingFailed() {

        }

        override fun onBondLost() {

        }

        override fun onReadRemoteRssi(rssi: Int, status: Int) {
            println("rssi is $rssi")
        }

        override fun onMtuChanged(mtu: Int, status: Int) {
            println("MTU is set to $mtu")
        }
    }

    /**
     * This method used to check whether the device services are refreshed or not after OTA. In Android devices when device is paired services won't refresh because
     * of cache mechanism. So every time after OTA we need to refresh the services, to validate this we are using this method.
     */
    private fun checkIsOnlyDFUServiceAvailbleORNot(
        services: List<BluetoothGattService>,
        requiredServices: HashSet<String>,
        config: DeviceConfig
    ): Boolean {
        var normalServiceAvailble = false
        var dfuServiceAvailble = false
        services.forEach {
            if (!normalServiceAvailble) {
                normalServiceAvailble = requiredServices.contains(it.uuid.toString())
            }
            if (!dfuServiceAvailble) {
                dfuServiceAvailble = config.dfuService.contains(it.uuid.toString())
            }
        }
        return !normalServiceAvailble && dfuServiceAvailble
    }

    private fun checkIsRequiredServicesAvailableOrNot(
        services: List<BluetoothGattService>,
        requiredServices: HashSet<String>
    ): Boolean {
        var normalServiceAvailble = false
        services.forEach {
            if (!normalServiceAvailble) {
                normalServiceAvailble = requiredServices.contains(it.uuid.toString())
            }
        }
        return normalServiceAvailble
    }

    fun commandExecute(taskPackage: TaskPackage?) {
        taskPackage?.let { task ->
            if (task.performWrite()) {
                if (task.responseWillNotify) {
                    writeToCharacteristicWaitToNotify(
                        task.getWriteIdentifier(),
                        task.getWriteData(),
                        task.key
                    )
                } else {
                    writeToCharacteristic(task.getWriteIdentifier(), task.getWriteData(), task.key)
                }
            }
            if (task.performRead()) {
                readFromCharacteristic(task.getReadIdentifier())
            }
        }
    }

    fun readRSSI() {
        bluetoothPeripheral.readRemoteRssi()
    }

    fun requestMTU() {
        bluetoothPeripheral.requestMtu(config.mtu)
    }

    fun customRequestMTU(mtu: Int = 120) {
        bluetoothPeripheral.requestMtu(config.mtu)
    }

    private fun writeToCharacteristicWaitToNotify(
        identifier: String,
        bytes: ByteArray,
        key: UUID?
    ) {
        localCharacteristics[identifier]?.let {
            bluetoothPeripheral.writeCharacteristic(
                it,
                bytes,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                true,
                key
            )
        }
    }

    private fun writeToCharacteristic(identifier: String, bytes: ByteArray, key: UUID?) {
        localCharacteristics[identifier]?.let {
            bluetoothPeripheral.writeCharacteristic(
                it,
                bytes,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                false,
                key
            )
        }
    }

    private fun readFromCharacteristic(identifier: String) {
        localCharacteristics[identifier]?.let {
            bluetoothPeripheral.readCharacteristic(it)
        }
    }

    private fun readFromDescriptor(identifier: String) {
        localDescriptor[identifier]?.let {
            bluetoothPeripheral.readDescriptor(it)
        }
    }

    private fun writeToDescriptor(identifier: String, bytes: ByteArray) {
        localDescriptor[identifier]?.let {
            bluetoothPeripheral.writeDescriptor(it, bytes)
        }
    }

    fun connect(bluetoothCentral: BluetoothCentral) {
        bluetoothPeripheral.pinPairing = config.pinPairing
        bluetoothPeripheral.notifyEnable = config.notify
        bluetoothCentral.connectPeripheral(
            peripheral = bluetoothPeripheral,
            peripheralCallback = bluetoothPeripheralCallback
        )
    }
}

interface DeviceConnectionCallback {
    fun connecting()
    fun connected()
    fun failed()
    fun disconnected()
    fun disconnecting()
}

interface BandStateCallback {
    fun none()
    fun ready()
    fun checkingFOTA()
    fun updatedFOTA()
    fun dataSyncStart()
    fun available()
}

enum class BandState(val value: Int) {
    NONE(-1),
    READY(0),
    FOTA_UPDATE(1),
    AVAILABLE(2),
    DATA_SYNC(3),
    WATCH_FACE_UPLOAD(4),
    ;
}