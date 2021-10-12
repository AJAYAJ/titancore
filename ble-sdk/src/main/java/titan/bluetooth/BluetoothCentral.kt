/*
 *   Copyright (c) 2019 Martijn van Welie
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *   SOFTWARE.
 *
 */
package titan.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.titan.log.EventType
import com.titan.logger.appLogger
import com.titan.logger.bleLogger
import titan.bluetooth.BluetoothPeripheral.InternalCallback
import java.util.*
import kotlin.collections.ArrayList

private const val TAG = "BT_Central"

/**
 * Central class to connect and communicate with bluetooth peripherals.
 */

/**
 * Construct a new BluetoothCentral object
 *
 * @param context                  Android application environment.
 * @param bluetoothCentralCallback the callback to call for updates
 * @param callBackHandler          Handler to use for callbacks.
 */
class BluetoothCentral(
    private val context: Context,
    private val bluetoothCentralCallback: BluetoothCentralCallback,
    private val callBackHandler: Handler
) {

    private var connectNow = false
    private var removePairing = false

    fun init() {
        // Register for broadcasts on BluetoothAdapter state change
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(adapterStateReceiver, filter)
    }

    private var keepConnectionAlive = true
    private var bluetoothScanner: BluetoothLeScanner? = null
    private var peripheral: BluetoothPeripheral? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    private val connectLock = Any()
    private val scanSettings: ScanSettings by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .setReportDelay(0L)
                .build()
        } else {
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0L)
                .build()
        }
    }

    private var expectingBluetoothOffDisconnects = false
    private var disconnectRunnable: Runnable = Runnable {
        Log.e(
            TAG,
            "bluetooth turned off but no automatic disconnects happening, so doing it ourselves"
        )
        bleLogger(
            TAG.plus(" bluetooth turned off but no automatic disconnects happening, so doing it ourselves"),
            type = EventType.ERROR
        )
        cancelAllConnectionsWhenBluetoothOff()
    }

    private val scanCallbackForUserSelection: ScanCallback = object : ScanCallback() {
        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            synchronized(this) {
                callBackHandler.post {
                    val peripheral = BluetoothPeripheral(
                        context,
                        result.device,
                        internalCallback,
                        callBackHandler
                    )
                    bluetoothCentralCallback.onDiscoveredPeripheral(peripheral, result)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            callBackHandler.post { bluetoothCentralCallback.onScanFailed(errorCode) }
        }
    }

    fun keepConnectionAlive(alwaysAlive: Boolean) {
        keepConnectionAlive = alwaysAlive
        startConnection()
    }

    private val internalCallback: InternalCallback = object : InternalCallback {
        override fun connecting(peripheral: BluetoothPeripheral) {
            callBackHandler.post {
                bluetoothCentralCallback.onConnectingPeripheral(peripheral)
            }
        }

        override fun connected(peripheral: BluetoothPeripheral) {
            mainHandler.removeCallbacks(connectionInitiateRunnable)
            mainHandler.removeCallbacks(connectionTimeoutRunnable)
            callBackHandler.post { bluetoothCentralCallback.onConnectedPeripheral(peripheral) }
        }

        override fun notifyFailed(peripheral: BluetoothPeripheral) {
            bleLogger("notify failed")
        }

        override fun disconnected(
            peripheral: BluetoothPeripheral,
            status: Int
        ) {
            bleLogger("disconnected ${peripheral.address}")
            Log.i(TAG, "disconnected '${peripheral.name}' (${peripheral.address})")
            if (expectingBluetoothOffDisconnects) {
                mainHandler.removeCallbacks(disconnectRunnable)
                expectingBluetoothOffDisconnects = false
            } else {
                if (keepConnectionAlive && this@BluetoothCentral.peripheral != null) {
                    startConnection()
                }
            }
            callBackHandler.post {
                bluetoothCentralCallback.onDisconnectedPeripheral(
                    peripheral,
                    status
                )
            }
            if (removePairing) {
                removePairing = false
                this@BluetoothCentral.peripheral = null
            }
        }

        override fun bondingStarted() {
            bleLogger("bondingStarted")
            startPairingPopupHack()
        }

        override fun ready(peripheral: BluetoothPeripheral) {
            bleLogger("ready")
            mainHandler.removeCallbacks(connectionTimeoutRunnable)
            callBackHandler.post {
                bluetoothCentralCallback.peripheralIsReady(peripheral)
            }
        }
    }

    /**
     * Closes BluetoothCentral and cleans up internals. BluetoothCentral will not work anymore after this is called.
     */
    fun unpair() {
        peripheral = null
        context.unregisterReceiver(adapterStateReceiver)
    }

    /**
     * Scan for peripherals that advertise at least one of the specified service UUIDs.
     *
     * @param serviceUUIDs an array of service UUIDs
     */
    fun scanForPeripherals(serviceUUIDs: List<String>) {
        // Check is BLE is available, enabled and all permission granted
        bleLogger(
            TAG.plus(" Start Scan request got. Is BLE in Ready State $isBleReady"),
            type = EventType.INFO
        )
        if (!isBleReady) return

        val filters: MutableList<ScanFilter> = ArrayList()
        for (serviceUUID in serviceUUIDs) {
            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(UUID.fromString(serviceUUID)))
                .build()
            filters.add(filter)
        }
        bleLogger(
            TAG.plus(" Scan peripherals with services ${serviceUUIDs.joinToString()}"),
            type = EventType.INFO
        )

        bluetoothScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
        bluetoothScanner?.startScan(filters, scanSettings, scanCallbackForUserSelection)
        Log.i(TAG, "scan started")
        bleLogger(TAG.plus(" Scan Started.$scanSettings"), type = EventType.INFO)
    }

    /**
     * Stop scanning for peripherals.
     */
    fun stopScan() {
        try {
            if (isBleEnabled) {
                bluetoothScanner?.stopScan(scanCallbackForUserSelection)
                Log.i(TAG, "scan stopped")
                bleLogger(TAG.plus(" Scan Stopped."), type = EventType.INFO)
            }
        } catch (e: Exception) {
            appLogger(e.localizedMessage.orEmpty())
        }
    }

    fun connectPeripheral(
        peripheral: BluetoothPeripheral,
        peripheralCallback: BluetoothPeripheralCallback
    ) {
        keepConnectionAlive = true
        synchronized(connectLock) {
            Log.i(
                TAG,
                "Connection request to ${this.peripheral?.address} - ${this.peripheral?.getState()}"
            )
            bleLogger("Connection request to ${this.peripheral?.address} - ${this.peripheral?.getState()}")
            if (this.peripheral != null || this.peripheral?.address == peripheral.address) {
                if (this.peripheral?.getState() != ConnectionState.STATE_CONNECTED &&
                    this.peripheral?.getState() != ConnectionState.STATE_CONNECTING
                ) {
                    connectNow = true
                    startConnection()
                }
                return
            }
            // It is all looking good! Set the callback and prepare to connect
            peripheral.peripheralCallback = peripheralCallback
            this.peripheral = peripheral
            connectNow = true
            startConnection()
        }
    }

    fun disconnect(connectNow: Boolean, removePairing: Boolean = false) {
        bleLogger("disconnect")
        this.connectNow = connectNow
        this.removePairing = removePairing
        this.peripheral?.disconnect()
    }

    fun createPeripheral(peripheralAddress: String?): BluetoothPeripheral? {
        if (!BluetoothAdapter.checkBluetoothAddress(peripheralAddress)) {
            Log.e(
                TAG,
                "$peripheralAddress is not a valid address. Make sure all alphabetic characters are uppercase."
            )
            return null
        }
        return when (peripheral?.address) {
            peripheralAddress -> {
                peripheral
            }
            else -> {
                BluetoothPeripheral(
                    context,
                    BluetoothAdapter.getDefaultAdapter().getRemoteDevice(peripheralAddress),
                    internalCallback,
                    callBackHandler
                )
            }
        }
    }

    private val isBleReady: Boolean
        get() {
            if (isBleSupported) {
                if (isBleEnabled) {
                    return permissionsGranted()
                }
            }
            return false
        }

    private val isBleSupported: Boolean
        get() {
            if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                return true
            }
            Log.e(TAG, "BLE not supported")
            bleLogger(TAG.plus(" BLE not supported."), type = EventType.ERROR)
            return false
        }

    private val isBleEnabled: Boolean
        get() {
            if (BluetoothAdapter.getDefaultAdapter().isEnabled) {
                return true
            }
            Log.e(TAG, "Bluetooth disabled")
            bleLogger("Bluetooth disabled")
            return false
        }

    private fun permissionsGranted(): Boolean {
        val targetSdkVersion = context.applicationInfo.targetSdkVersion
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetSdkVersion >= Build.VERSION_CODES.Q) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "no ACCESS_FINE_LOCATION permission, cannot scan")
                false
            } else true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "no ACCESS_FINE_LOCATION permission, cannot scan")
                false
            } else true
        } else {
            true
        }
    }

    private fun startConnection() {
        bleLogger("startConnection: connect now: $connectNow, alive: $keepConnectionAlive")
        Log.i(TAG, "startConnection: connect now: $connectNow, alive: $keepConnectionAlive")
        if (peripheral != null) {
            mainHandler.removeCallbacks(connectionTimeoutRunnable)
            mainHandler.removeCallbacks(connectionInitiateRunnable)
            mainHandler.postDelayed(
                connectionInitiateRunnable,
                if (!connectNow) CONNECTION_DELAY else 1000L
            )
        }
        connectNow = false
    }

    fun stopConnectionHandlers() {
        keepConnectionAlive = false
        mainHandler.removeCallbacks(connectionTimeoutRunnable)
        mainHandler.removeCallbacks(connectionInitiateRunnable)
    }

    private val connectionTimeoutRunnable = Runnable {
        bleLogger("connectionTimeoutRunnable")
        peripheral?.disconnect()
    }

    private val connectionInitiateRunnable = Runnable {
        bleLogger("connectionInitiateRunnable")
        if (this.peripheral?.getState() != ConnectionState.STATE_CONNECTED) {
            mainHandler.postDelayed(connectionTimeoutRunnable, CONNECTION_TIMEOUT)
        }
        peripheral?.connect()
    }

    /**
     * Remove bond for a peripheral.
     *
     * @return true if the peripheral was successfully unpaired or it wasn't paired, false if it was paired and removing it failed
     */
    fun removeBond(macAddress: String = ""): Boolean {
        bleLogger("removeBond")
        val result: Boolean
        var peripheralToUnBond: BluetoothDevice? = null

        // Get the set of bonded devices
        val bondedDevices = BluetoothAdapter.getDefaultAdapter().bondedDevices

        // See if the device is bonded
        val peripheralAddress = if (macAddress.isNotEmpty()) {
            macAddress
        } else {
            peripheral?.address
        }
        if (bondedDevices.size > 0) {
            for (device in bondedDevices) {
                if (device.address.equals(peripheralAddress, true)) {
                    peripheralToUnBond = device
                }
            }
        } else {
            bleLogger(
                TAG.plus(" Remove bond Request, but $peripheralAddress is not bounded"),
                type = EventType.ERROR
            )
            return true
        }

        // Try to remove the bond

        return if (peripheralToUnBond != null) {
            try {
                val method = peripheralToUnBond.javaClass.getMethod("removeBond")
                result = method.invoke(peripheralToUnBond) as Boolean
                if (result) {
                    Log.i(
                        TAG,
                        "Successfully removed bond for '${peripheralToUnBond.name}' Address-${peripheralAddress}"
                    )
                    bleLogger(
                        TAG.plus(" Successfully removed bond for '${peripheralToUnBond.name}' Address-${peripheralAddress}"),
                        type = EventType.INFO
                    )
                }
                result
            } catch (e: Exception) {
                Log.i(TAG, "could not remove bond")
                bleLogger(
                    TAG.plus(" could not remove bond. ${e.message ?: ""}"),
                    type = EventType.INFO
                )
                e.printStackTrace()
                false
            }
        } else {
            true
        }
    }

    /**
     * Make the pairing popup appear in the foreground by doing a 1 sec discovery.
     *
     * If the pairing popup is shown within 60 seconds, it will be shown in the foreground.
     */
    fun startPairingPopupHack() {
        // Check if we are on a Samsung device because those don't need the hack
        val manufacturer = Build.MANUFACTURER.toLowerCase(Locale.getDefault())
        if (manufacturer != "samsung") {
            BluetoothAdapter.getDefaultAdapter().startDiscovery()
            callBackHandler.postDelayed({
                Log.d(TAG, "popup hack completed")
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
            }, 1000)
        }
    }

    /**
     * Some phones, like Google/Pixel phones, don't automatically disconnect devices so this method does it manually
     */
    private fun cancelAllConnectionsWhenBluetoothOff() {
        bleLogger("cancelAllConnectionsWhenBluetoothOff")
        peripheral?.disconnectWhenBluetoothOff()
    }

    private fun startDisconnectionTimer() {
        bleLogger("startDisconnectionTimer")
        mainHandler.removeCallbacks(disconnectRunnable)
        mainHandler.postDelayed(disconnectRunnable, 1000)
    }

    private val adapterStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            val action = intent.action ?: return
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state =
                    intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                callBackHandler.post {
                    bluetoothCentralCallback.onBluetoothAdapterStateChanged(
                        state
                    )
                }
                handleAdapterState(state)
            }
        }
    }

    private fun handleAdapterState(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_OFF -> {
                if (peripheral != null) {
                    expectingBluetoothOffDisconnects = true
                    startDisconnectionTimer()
                }
            }
            BluetoothAdapter.STATE_TURNING_OFF -> {
                try {
                    expectingBluetoothOffDisconnects = true
                    if (isBleEnabled) {
                        bluetoothScanner?.stopScan(scanCallbackForUserSelection)
                    }
                    mainHandler.removeCallbacks(connectionTimeoutRunnable)
                    mainHandler.removeCallbacks(connectionInitiateRunnable)
                } catch (e: Exception) {
                    appLogger(e.localizedMessage.orEmpty())
                }
            }
            BluetoothAdapter.STATE_ON -> {
                expectingBluetoothOffDisconnects = false
                startConnection()
            }
            BluetoothAdapter.STATE_TURNING_ON -> {
                expectingBluetoothOffDisconnects = false
            }
        }
    }

    fun turnOnBluetooth() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            BluetoothAdapter.getDefaultAdapter().enable()
        }
    }

    fun bluetoothStatus(): Boolean {
        return BluetoothAdapter.getDefaultAdapter().isEnabled
    }

    companion object {
        private const val CONNECTION_TIMEOUT = 30000L
        private const val CONNECTION_DELAY = 10000L
    }
}