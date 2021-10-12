package titan.core.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.titan.logger.coreLogger
import titan.bluetooth.BluetoothCentral
import titan.bluetooth.BluetoothCentralCallback
import titan.bluetooth.BluetoothPeripheral
import titan.bluetooth.ProductInfo
import titan.core.products.BandState
import titan.core.products.TaskPackage
import titan.core.products.TitanWearable
import titan.core.products.reflex_2.Reflex2
import titan.core.products.reflex_3.Reflex3
import titan.core.products.reflex_slay.ReflexSlay
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashSet

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

enum class ReflexProducts constructor(val code: String) {
    REFLEX_1("REFLEX_1"),
    REFLEX_2("REFLEX_2"),
    REFLEX_BEAT("REFLEX_BEAT"),
    REFLEX_3("REFLEX_3"),
    REFLEX_SLAY("SLAY"),
    REFLEX_2C("REFLEX_2C"),
    ;

    companion object {
        fun getProduct(code: String?): ReflexProducts? {
            values().forEach {
                if (code == it.code) {
                    return it
                }
            }
            return null
        }

        fun getProductsList(): Array<ReflexProducts> {
            return arrayOf(REFLEX_SLAY, REFLEX_3, REFLEX_BEAT, REFLEX_2, REFLEX_1)
        }

        fun isReflex1(code: String?): Boolean {
            return arrayOf(REFLEX_1).contains(getProduct(code))
        }

        fun isReflex2(code: String?): Boolean {
            return arrayOf(REFLEX_1, REFLEX_2, REFLEX_BEAT, REFLEX_2C).contains(getProduct(code))
        }

        fun isReflex3(code: String?): Boolean {
            return arrayOf(REFLEX_3).contains(getProduct(code))
        }

        fun isReflexSlay(code: String?): Boolean {
            return arrayOf(REFLEX_SLAY).contains(getProduct(code))
        }
    }
}

class CommManager constructor(private val context: Context) {

    internal val scanListeners = HashSet<ScanListener>()
    internal val bluetoothStateListeners = HashSet<BluetoothStateListener>()

    private val scannedDevices = ConcurrentHashMap<String, BluetoothPeripheral>()
    private val products = ConcurrentHashMap<String, Product>()

    internal var wearable: TitanWearable? = null
    internal var isOTAInProgress: Boolean = false

    fun isConnected(): Boolean {
        return wearable?.isConnected() ?: false
    }

    private val handler = Handler(Looper.getMainLooper())

    private val productConfig: ProductConfig by lazy {
        try {
            val open = context.assets.open("products.json")
            val bArr = ByteArray(open.available())

            open.read(bArr)
            open.close()
            Gson().fromJson(
                String(bArr),
                ProductConfig::class.java
            )
        } catch (e: IOException) {
            throw IOException("Unable to load products json.")
        } catch (e: JsonParseException) {
            throw JsonParseException("Unable to configure products.")
        }
    }

    private val bluetoothCentral by lazy {
        BluetoothCentral(
            context = context,
            bluetoothCentralCallback = bluetoothCentralCallback,
            callBackHandler = handler
        )
    }

    fun supportedProducts(): List<ReflexProducts> {
        val set = HashSet<ReflexProducts>()
        productConfig.products.sortedByDescending { it.sortingID }.forEach {
            ReflexProducts.getProduct(it.code)?.let { rp ->
                set.add(rp)
            }
        }
        return set.toList()
    }

    private fun init() {
        bluetoothCentral.init()
    }

    fun isDeviceReady() = getBandState().value >= BandState.READY.value
    var canSyncDeviceInfo = true

    fun scan() {
        coreLogger("scan")
        clearScannedDevices()
        val serviceUUIDs = HashSet<String>()
        productConfig.products.forEach { product ->
            product.filter.UUIDs.forEach {
                serviceUUIDs.add(it.toUpperCase(Locale.getDefault()))
            }
            products[product.filter.name] = product
        }
        bluetoothCentral.scanForPeripherals(serviceUUIDs.toList())
        scanListeners.forEach { it.scanStarted() }
    }

    fun scanDFU() {
        coreLogger("scan dfu")
        clearScannedDevices()
        val serviceUUIDs = HashSet<String>()
        wearable?.getDFUService()?.let { serviceUUIDs.addAll(it) }
        productConfig.products.forEach { product ->
            products[product.filter.name] = product
        }
        bluetoothCentral.scanForPeripherals(serviceUUIDs.toList())
        scanListeners.forEach { it.scanStarted() }
    }

    fun stopScan() {
        coreLogger("stop scan")
        scanListeners.forEach { it.scanStopped() }
        bluetoothCentral.stopScan()
    }

    fun clearScannedDevices() {
        coreLogger("clear scanned devices")
        products.clear()
        scannedDevices.clear()
    }

    fun connect(address: String?, productCode: String?): Boolean {
        coreLogger("connect $address, $productCode wearable exists: ${wearable != null}")
        if (wearable == null) {
            bluetoothCentral.createPeripheral(address)?.let { peripheral ->
                peripheral.productInfo = productCode?.let { getProductInfo(it) }
                val titanWearable = TitanWearable(context, peripheral)
                titanWearable.connect(bluetoothCentral)
                wearable = titanWearable
                clearScannedDevices()
                return true
            }
        } else {
            wearable?.connect(bluetoothCentral)
            return true
        }
        return false
    }

    fun disconnect(connectNow: Boolean = false) {
        coreLogger("disconnect, connect now: $connectNow")
        bluetoothCentral.disconnect(connectNow)
    }

    fun removePairing() {
        coreLogger("remove pairing")
        bluetoothCentral.disconnect(connectNow = false, removePairing = true)
    }

    fun isRequiredServicesAvailable(): Boolean {
        return wearable?.isRequiredServicesAvailable() ?: false
    }

    @Suppress("unused")
    fun unBond(): Boolean {
        coreLogger("unbond")
        return bluetoothCentral.removeBond()
    }

    fun unBondByUsingMacAddress(macAddress: String): Boolean {
        coreLogger("unbond")
        return bluetoothCentral.removeBond(macAddress)
    }

    fun clearWearable() {
        coreLogger("clear wearable")
        removePairing()
        wearable = null
    }

    fun turnOnBluetooth() {
        coreLogger("turn on")
        bluetoothCentral.turnOnBluetooth()
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothCentral.bluetoothStatus()
    }

    fun executeCommand(body: () -> TaskPackage) {
        wearable?.commandExecute(body())
    }

    fun getDFUService() = wearable?.getDFUService()

    fun getFilterServiceForMainAndDfuDevice(): List<String> {
        val list: ArrayList<String> = arrayListOf()
        productConfig.products.forEach { it ->
            wearable?.productInfo?.getCode()?.let { code ->
                if (code == it.code) {
                    it.filter.UUIDs.forEach { uuid ->
                        list.add(uuid.toUpperCase(Locale.getDefault()))
                    }
                }
            }
        }
        getDFUService()?.let { it ->
            list.addAll(it)
        }
        return list
    }

    fun dfuSupported() = wearable?.isDFUSupported() ?: false

    companion object {
        private lateinit var commManager: CommManager

        fun init(context: Context) {
            commManager = CommManager(context)
            commManager.init()
        }

        fun getInstance(): CommManager {
            if (!::commManager.isInitialized) {
                throw NullPointerException("Communication manager is not initialized.")
            }
            return commManager
        }
    }

    fun getDeviceAddress(): String? {
        return wearable?.address
    }

    fun isOTAInProgress(): Boolean {
        return isOTAInProgress
    }

    fun setOTAState(inProgress: Boolean) {
        isOTAInProgress = inProgress
    }

    fun getDeviceSettings(): Settings {
        return wearable?.settings ?: Settings(
            notifications = true,
            about = true,
            alarm = true,
            autoSleep = false,
            antiLost = false,
            autoHeartRate = true,
            bandLayout = false,
            dnd = true,
            eventLog = true,
            factoryReset = false,
            findBand = false,
            findPhone = true,
            liftToView = true,
            ota = true,
            pair = true,
            sedentaryAlert = true,
            sleepGoal = true,
            stepsGoal = true,
            unitSystem = true,
            watchFace = true,
            multiSportGoal = true,
            batteryOptimization = true
        )
    }

    fun getAboutBandUrls(): AboutBand {
        return wearable?.aboutBand ?: AboutBand(
            termsAndConditionsURL = "https://www.fastrack.in/content/privacy-policy",
            privacyPolicyURL = "https://www.fastrack.in/content/privacy-policy",
            bandUserManualURL = "",
            aboutTheProductURL = "https://www.fastrack.in/shop/watch-smart-wearables",
            aboutCompanyURL = "https://www.titancompany.in/"
        )
    }

    fun getDeviceOptions(): Options? {
        return wearable?.getProductOptions()
    }

    private val bluetoothCentralCallback = object : BluetoothCentralCallback {
        override fun onConnectingPeripheral(peripheral: BluetoothPeripheral) {
            coreLogger("onConnectingPeripheral")
            wearable?.getConnectionCallback()?.connecting()
        }

        override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
            coreLogger("onConnectedPeripheral")
            wearable?.getConnectionCallback()?.connected()
        }

        override fun onConnectionFailed(peripheral: BluetoothPeripheral, status: Int) {
            coreLogger("onConnectionFailed")
            wearable?.getConnectionCallback()?.failed()
        }

        override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral, status: Int) {
            coreLogger("onDisconnectedPeripheral")
            wearable?.getConnectionCallback()?.disconnected()
        }

        override fun onDiscoveredPeripheral(
            peripheral: BluetoothPeripheral,
            scanResult: ScanResult
        ) {
            peripheral.name?.let { deviceName ->
                val scannedDevice = scannedDevices[peripheral.address]
                if (scannedDevice == null) {
                    scanListeners.forEach { scanListener ->
                        products.forEach { (name, product) ->
                            if (deviceName.startsWith(name, true)) {
                                peripheral.productInfo = getProductInfo(product.code)
                                scannedDevices[peripheral.address] = peripheral
                                scanListener.onDeviceFound(
                                    displayName = deviceName,
                                    productName = product.name,
                                    address = peripheral.address,
                                    productCode = product.code,
                                    rssi = scanResult.rssi,
                                    productGroup = productConfig.group
                                )
                            }
                        }
                    }
                } else {
                    scanListeners.forEach { it.updateRSSI(peripheral.address, scanResult.rssi) }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            scanListeners.forEach { it.scanStopped() }
        }

        override fun onBluetoothAdapterStateChanged(state: Int) {
            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    bluetoothStateListeners.forEach { it.on() }
                }
                BluetoothAdapter.STATE_OFF -> {
                    bluetoothStateListeners.forEach { it.off() }
                }
            }
        }

        override fun peripheralIsReady(peripheral: BluetoothPeripheral) {
            coreLogger("peripheralIsReady")
            wearable?.getBandStateCallback()?.ready()
        }
    }

    private fun getProductInfo(productCode: String): ProductInfo {
        return when (ReflexProducts.getProduct(productCode)) {
            ReflexProducts.REFLEX_1 -> Reflex2.getProductInfo(productCode)
            ReflexProducts.REFLEX_2 -> Reflex2.getProductInfo(productCode)
            ReflexProducts.REFLEX_BEAT -> Reflex2.getProductInfo(productCode)
            ReflexProducts.REFLEX_3 -> Reflex3.getProductInfo()
            ReflexProducts.REFLEX_SLAY -> ReflexSlay.getProductInfo()
            ReflexProducts.REFLEX_2C -> Reflex2.getProductInfo(productCode)
            else -> throw Exception("$productCode is not supported")
        }
    }

    fun requestMTU(i: Int) {
        wearable?.customRequestMTU(i)
    }

    fun getBandState(): BandState {
        return wearable?.getBandState() ?: BandState.NONE
    }

    fun checkingFOTAStatus() {
        wearable?.getBandStateCallback()?.checkingFOTA()
    }

    fun updatedFOTA() {
        wearable?.getBandStateCallback()?.updatedFOTA()
    }

    fun dataSyncStart() {
        wearable?.getBandStateCallback()?.dataSyncStart()
    }

    fun bandAvailable() {
        wearable?.getBandStateCallback()?.available()
    }

    fun createWearable(address: String, productCode: String) {
        if (wearable == null) {
            bluetoothCentral.createPeripheral(address)?.let { peripheral ->
                peripheral.productInfo = productCode?.let { getProductInfo(it) }
                val titanWearable = TitanWearable(context, peripheral)
                wearable = titanWearable
                clearScannedDevices()
            }
        }
    }

    fun stopConnectionHandlers() {
        setOTAState(true)
        bluetoothCentral.stopConnectionHandlers()
    }
}