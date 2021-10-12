package titan.core.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.os.Build
import android.os.ParcelUuid
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

class DFUManager {
    private val commManager by lazy {
        CommManager.getInstance()
    }
    private var bluetoothScanner: BluetoothLeScanner? = null
    private val scannedDevices = ConcurrentHashMap<String, DFUScanModel>()

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

    fun stopScan() {
        bluetoothScanner?.stopScan(scanCallback)
    }

    private fun clearScannedDevices() {
        scannedDevices.clear()
    }

    fun getScannedDevices() = scannedDevices.values

    fun scanDFUDevice() {
        stopScan()
        clearScannedDevices()
        val filters: MutableList<ScanFilter> = ArrayList()
        commManager.getDFUService()?.let {
            for (serviceUUID in it) {
                val filter = ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid(UUID.fromString(serviceUUID)))
                        .build()
                filters.add(filter)
            }
        }
        bluetoothScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
        bluetoothScanner?.startScan(filters, scanSettings, scanCallback)
    }

    fun scanDeviceAfterDFU() {
        stopScan()
        clearScannedDevices()
        val filters: MutableList<ScanFilter> = ArrayList()
        commManager.getDFUService()?.let {
            for (serviceUUID in it) {
                val filter = ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid(UUID.fromString(serviceUUID)))
                        .build()
                filters.add(filter)
            }
        }
        bluetoothScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
        bluetoothScanner?.startScan(filters, scanSettings, scanCallback)
    }

    fun scanDFU() {
        stopScan()
        clearScannedDevices()
        val filters: MutableList<ScanFilter> = ArrayList()
        commManager.getDFUService()?.let {
            for (serviceUUID in it) {
                val filter = ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid(UUID.fromString(serviceUUID)))
                        .build()
                filters.add(filter)
            }
        }
        bluetoothScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
        bluetoothScanner?.startScan(filters, scanSettings, scanCallback)
    }

    fun scanForNormalDeviceAndDFUDevice() {
        stopScan()
        clearScannedDevices()
        val filters: MutableList<ScanFilter> = ArrayList()
        commManager.getFilterServiceForMainAndDfuDevice()?.let {
            for (serviceUUID in it) {
                val filter = ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid(UUID.fromString(serviceUUID)))
                        .build()
                filters.add(filter)
            }
        }
        bluetoothScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
        bluetoothScanner?.startScan(filters, scanSettings, scanCallback)
    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(
                callbackType: Int,
                result: ScanResult
        ) {
            synchronized(this) {
                val scannedDevice = scannedDevices[result.device.address]
                if (scannedDevice == null && result.device != null && result.device.name != null && result.device.name.contains("FT_Reflex3")) {
                    println(result.device.name)
                    scannedDevices[result.device.address] =
                            DFUScanModel(result.device.address, result.device.name)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
        }
    }
}

data class DFUScanModel(
        val address: String,
        val name: String?
)