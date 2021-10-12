package titan.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.titan.log.EventType
import com.titan.logger.bleLogger
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

/**
 * Represents a remote Bluetooth peripheral and replaces BluetoothDevice and BluetoothGatt
 *
 *
 * A [BluetoothPeripheral] lets you create a connection with the peripheral or query information about it.
 * This class is a wrapper around the [BluetoothDevice] and takes care of operation queueing, some Android bugs, and provides several convenience functions.
 */

private const val TAG = "BT_Peripheral"

enum class CommandResponseState {
    COMPLETED,
    INCOMPLETE,
    INCOMPATIBLE
}

enum class CommandType {
    COMMAND_GENERIC,
    COMMAND_SET_BOND,
    COMMAND_SET_NOTIFY,
    COMMAND_TO_WRITE_WITH_NOTIFY_RESPONSE,
    COMMAND_TO_WRITE,
    COMMAND_TO_READ,
}

enum class CommandState(val value: Int) {
    COMMAND_IN_PROGRESS(1),
    COMMAND_IN_RETRY(1),
    COMMAND_EXECUTED(2),
    DATA_RECEIVING(2),
    DATA_UNKNOWN(3),
    DATA_RECEIVED(3)
}

enum class ConnectionState(val value: Int) {
    STATE_FAILED(-1),
    STATE_DISCONNECTED(0),
    STATE_CONNECTING(1),
    STATE_CONNECTED(2),
    STATE_DISCONNECTING(3)
}

data class CommandStatus(
    val key: UUID?,
    val commandType: CommandType,
    var commandState: CommandState
)

private data class BTCommand(
    val runnable: Runnable,
    val key: UUID?,
    val responseState: CommandType
)

class BluetoothPeripheral(
    private val context: Context,
    private val device: BluetoothDevice,
    private val listener: InternalCallback,
    private val callbackHandler: Handler
) {
    private val handler = Handler(Looper.getMainLooper())
    var peripheralCallback: BluetoothPeripheralCallback? = null
    var pinPairing = false
    var notifyEnable = false
    var productInfo: ProductInfo? = null
    private val commandQueue: Queue<BTCommand> = ConcurrentLinkedQueue()

    private var bondLost = false
    private var manuallyBonding = false

    private var commandStatus: CommandStatus? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private val commandTimeoutRunnable = Runnable {
        commandStatus?.commandState = CommandState.DATA_RECEIVED
        completedCommand()
    }

    /**
     * Returns the connection state of the peripheral.
     *
     *
     * Possible values for the connection state are:
     * [.STATE_CONNECTED],
     * [.STATE_CONNECTING],
     * [.STATE_DISCONNECTED],
     * [.STATE_DISCONNECTING].
     * [.STATE_READY].
     *
     * @return the connection state.
     */
    private var state: ConnectionState = ConnectionState.STATE_DISCONNECTED

    private var nrTries = 0
    private var currentWriteBytes: ByteArray? = null
    private val notifyingCharacteristics: MutableSet<UUID> = HashSet()
    private val pendingNotifyCharacteristics: MutableSet<BluetoothGattCharacteristic> = HashSet()
    private var notificationEnableRetryCount = 2
    private val mainHandler = Handler(Looper.getMainLooper())
    private var connectTimestamp: Long = 0
    private var cachedName: String? = null

    private val timeoutRunnable: Runnable = Runnable {
        disconnect()
    }

    private val notificationEnableRunnable: Runnable = Runnable {
        disconnect()
    }

    private val discoverServicesRunnable = Runnable {
        if (bluetoothGatt?.discoverServices() == false) {
            Log.e(TAG, "discoverServices failed to start")
        }
    }

    fun getState(): ConnectionState = state

    private val bluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            Log.i(TAG, "onConnectionStateChange: $newState")
            val timePassed = SystemClock.elapsedRealtime() - connectTimestamp
            mainHandler.removeCallbacks(timeoutRunnable)
            if (status == GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        val bondState = device.bondState
                        Log.i(
                            TAG,
                            "connected to ${device.name} (${bondStateToString(bondState)}) in ${timePassed / 1000}s"
                        )
                        successfullyConnected(bondState)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        successfullyDisconnected()
                    }
                    BluetoothProfile.STATE_DISCONNECTING -> {
                        state = ConnectionState.STATE_DISCONNECTING
                    }
                    BluetoothProfile.STATE_CONNECTING -> {
                        Log.i(TAG, "peripheral is connecting")
                        state = ConnectionState.STATE_CONNECTING
                        listener.connecting(this@BluetoothPeripheral)
                    }
                    else -> {
                        Log.e(TAG, "unknown state received")
                        bleLogger("unknown state received")
                    }
                }
            } else {
                connectionStateChangeUnsuccessful(status, newState, timePassed)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == GATT_INTERNAL_ERROR) {
                Log.e(TAG, "service discovery failed due to internal error, disconnecting")
                disconnect()
                return
            }
            state = ConnectionState.STATE_CONNECTED
            listener.connected(this@BluetoothPeripheral)
            val services = gatt.services
            Log.i(TAG, "discovered ${services.size} services for '$name'")
            pendingNotifyCharacteristics.clear()
            callbackHandler.post {
                services.forEach { service ->
                    service.characteristics.forEach { characteristic ->
                        if (peripheralCallback?.isNotification(characteristic) == true) {
                            pendingNotifyCharacteristics.add(characteristic)
                        }
                    }
                }
                peripheralCallback?.onServicesDiscovered(services)
                enableNotifications()
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            val parentCharacteristic = descriptor.characteristic
            if (status != GATT_SUCCESS) {
                Log.e(
                    TAG,
                    "write descriptor failed value ${currentWriteBytes?.contentToString()}, device: ${address}, characteristic: ${parentCharacteristic.uuid}"
                )
                bleLogger(
                    "write descriptor failed value ${currentWriteBytes?.contentToString()}, device: ${address}",
                    type = EventType.ERROR
                )
            }
            // Check if this was the Client Configuration Descriptor
            if (descriptor.uuid == UUID.fromString(CCC_DESCRIPTOR_UUID)) {
                if (status == GATT_SUCCESS) {
                    notificationEnableRetryCount = 2
                    if (pendingNotifyCharacteristics.isNotEmpty()) {
                        val characteristic = pendingNotifyCharacteristics.firstOrNull()
                        characteristic?.let {
                            pendingNotifyCharacteristics.remove(it)
                        }
                    }
                    if (pendingNotifyCharacteristics.isEmpty()) {
                        callbackHandler.post {
                            peripheralCallback?.onNotificationStateUpdate(
                                parentCharacteristic,
                                status
                            )
                            if (commandStatus?.commandType == CommandType.COMMAND_SET_NOTIFY) {
                                handler.removeCallbacks(notificationEnableRunnable)
                                listener.ready(this@BluetoothPeripheral)
                                commandStatus?.commandState = CommandState.DATA_RECEIVED
                            }
                        }
                    } else {
                        enableNotifications()
                    }
                    val value = descriptor.value
                    if (value != null) {
                        if (value[0] != 0.toByte()) {
                            // Notify set to on, add it to the set of notifying characteristics
                            notifyingCharacteristics.add(parentCharacteristic.uuid)
                            if (notifyingCharacteristics.size > MAX_NOTIFYING_CHARACTERISTICS) {
                                Log.e(
                                    TAG,
                                    "too many ${notifyingCharacteristics.size} notifying characteristics. The maximum Android can handle is $MAX_NOTIFYING_CHARACTERISTICS"
                                )
                                bleLogger(
                                    "too many ${notifyingCharacteristics.size} notifying characteristics. The maximum Android can handle is $MAX_NOTIFYING_CHARACTERISTICS",
                                    type = EventType.ERROR
                                )
                            }
                        } else {
                            // Notify was turned off, so remove it from the set of notifying characteristics
                            notifyingCharacteristics.remove(parentCharacteristic.uuid)
                        }
                    }
                } else {
                    if (notificationEnableRetryCount > 0) {
                        enableNotifications()
                    } else {
                        disconnect()
                    }
                    notificationEnableRetryCount--
                }
                callbackHandler.post {
                    completedCommand()
                }
            } else {
                callbackHandler.post {
                    peripheralCallback?.onDescriptorWrite(
                        currentWriteBytes,
                        descriptor,
                        status
                    )
                    completedCommand()
                }
            }
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status != GATT_SUCCESS) {
                Log.e(
                    TAG,
                    "writing ${descriptor.value?.contentToString()} to descriptor ${descriptor.uuid} failed for device '$address'"
                )
                bleLogger(
                    "writing ${descriptor.value?.contentToString()} to descriptor ${descriptor.uuid} failed for device '$address'",
                    type = EventType.ERROR
                )
            }
            val value = copyOf(descriptor.value)
            callbackHandler.post {
                peripheralCallback?.onDescriptorRead(
                    value,
                    descriptor,
                    status
                )
            }
            completedCommand()
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = copyOf(characteristic.value)
            Log.d(
                TAG,
                "Rx: ${characteristic.uuid}, data: ${value.toHexString()}"
            )
            try {
                bleLogger(
                    "Rx: ${value.toHexString()}",
                    type = EventType.INFO
                )
            } catch (e: Exception) {
                /*Nothing do here.*/
            }
            callbackHandler.post {
                when (peripheralCallback?.onCharacteristicUpdate(
                    value,
                    characteristic,
                    GATT_SUCCESS,
                    true,
                    commandStatus?.key
                )) {
                    CommandResponseState.COMPLETED -> {
                        commandStatus?.commandState = CommandState.DATA_RECEIVED
                        completedCommand()
                    }
                    CommandResponseState.INCOMPLETE -> {
                        commandStatus?.commandState = CommandState.DATA_RECEIVING
                        commandTimeout(2000L)
                    }
                    CommandResponseState.INCOMPATIBLE -> {
                        commandStatus?.commandState = CommandState.DATA_UNKNOWN
                        commandTimeout(2000L)
                    }
                    else -> {
                        commandStatus?.commandState = CommandState.DATA_UNKNOWN
                        commandTimeout(2000L)
                    }
                }
                if (commandStatus == null) {
                    commandTimeout(2000L)
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status != GATT_SUCCESS) {
                if (status == GATT_AUTH_FAIL || status == GATT_INSUFFICIENT_AUTHENTICATION) {
                    // Characteristic encrypted and needs bonding,
                    // So retry operation after bonding completes
                    // This only seems to happen on Android 5/6/7
                    Log.w(TAG, "read needs bonding, bonding in progress")
                    bleLogger(
                        "read needs bonding, bonding in progress",
                        type = EventType.ERROR
                    )
                    return
                } else {
                    Log.e(
                        TAG,
                        "read failed for characteristic: ${characteristic.uuid}, status $status"
                    )
                    bleLogger(
                        "read failed, status $status",
                        type = EventType.ERROR
                    )
                    commandStatus?.commandState = CommandState.DATA_UNKNOWN
                    completedCommand()
                    return
                }
            }
            val value = copyOf(characteristic.value)
            Log.d(
                TAG,
                "Rx: ${characteristic.uuid}, data: ${value.toHexString()}"
            )
            try {
                bleLogger(
                    "Rx: ${value.toHexString()}",
                    type = EventType.INFO
                )
            } catch (e: Exception) {
                /*Nothing do here.*/
            }
            callbackHandler.post {
                when (peripheralCallback?.onCharacteristicUpdate(
                    value,
                    characteristic,
                    GATT_SUCCESS,
                    true,
                    commandStatus?.key
                )
                ) {
                    CommandResponseState.COMPLETED -> {
                        commandStatus?.commandState = CommandState.DATA_RECEIVED
                        completedCommand()
                    }
                    CommandResponseState.INCOMPLETE -> {
                        commandStatus?.commandState = CommandState.DATA_RECEIVING
                        reExecuteCommand()
                        commandTimeout(2000L)
                    }
                    CommandResponseState.INCOMPATIBLE -> {
                        commandStatus?.commandState = CommandState.DATA_UNKNOWN
                        commandTimeout(2000L)
                    }
                    else -> {
                        commandStatus?.commandState = CommandState.DATA_UNKNOWN
                        commandTimeout(2000L)
                    }
                }
                if (commandStatus == null) {
                    commandTimeout(2000L)
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status != GATT_SUCCESS) {
                if (status == GATT_AUTH_FAIL || status == GATT_INSUFFICIENT_AUTHENTICATION) {
                    // Characteristic encrypted and needs bonding,
                    // So retry operation after bonding completes
                    // This only seems to happen on Android 5/6/7
                    Log.i(TAG, "write needs bonding, bonding in progress")
                    bleLogger(
                        "write needs bonding, bonding in progress",
                        type = EventType.ERROR
                    )
                    return
                } else {
                    Log.e(
                        TAG,
                        "writing ${currentWriteBytes?.toHexString()} to characteristic ${characteristic.uuid} failed, status $status"
                    )
                    bleLogger(
                        "writing ${currentWriteBytes?.toHexString()} to characteristic ${characteristic.uuid} failed, status $status",
                        type = EventType.ERROR
                    )

                }
            } else {
                Log.i(
                    TAG,
                    "writing ${currentWriteBytes?.toHexString()} to characteristic ${characteristic.uuid} execute."
                )
            }
            val value = currentWriteBytes?.let { copyOf(it) }
            currentWriteBytes = null
            callbackHandler.post {
                peripheralCallback?.onCharacteristicWrite(
                    value,
                    characteristic,
                    status
                )
            }
            if (commandStatus?.commandType == CommandType.COMMAND_TO_WRITE) {
                commandStatus?.commandState = CommandState.DATA_RECEIVED
                completedCommand()
            } else {
                commandStatus?.commandState = CommandState.COMMAND_EXECUTED
            }
        }

        override fun onReadRemoteRssi(
            gatt: BluetoothGatt,
            rssi: Int,
            status: Int
        ) {
            callbackHandler.post {
                peripheralCallback?.onReadRemoteRssi(
                    rssi,
                    status
                )
            }
            commandStatus?.commandState = CommandState.DATA_RECEIVED
            completedCommand()
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            callbackHandler.post {
                peripheralCallback?.onMtuChanged(
                    mtu,
                    status
                )
            }
            commandStatus?.commandState = CommandState.DATA_RECEIVED
            completedCommand()
        }
    }

    private fun successfullyConnected(bondstate: Int) {
        if (pinPairing && bondstate == BOND_BONDED) {
            delayedDiscoverServices(getServiceDiscoveryDelay(bondstate))
        } else if (pinPairing && bondstate != BOND_BONDED) {
            listener.bondingStarted()
            createBond()
            Log.i(TAG, "waiting for bonding to complete")
            bleLogger("waiting for bonding to complete")
        } else {
            delayedDiscoverServices(getServiceDiscoveryDelay(bondstate))
        }
    }

    private fun delayedDiscoverServices(delay: Long) {
        mainHandler.postDelayed(discoverServicesRunnable, delay)
    }

    private fun getServiceDiscoveryDelay(bondState: Int): Long {
        var delayWhenBonded: Long = 0
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            // It seems delays when bonded are only needed in versions Nougat or lower
            // This issue was observed on a Nexus 5 (M) and Sony Xperia L1 (N) when connecting to a A&D UA-651BLE
            // The delay is needed when devices have the Service Changed Characteristic.
            // If they don't have it the delay isn't needed but we do it anyway to keep code simple
            delayWhenBonded = 1000L
        }
        return if (bondState == BOND_BONDED) delayWhenBonded else 0
    }

    private fun successfullyDisconnected() {
        state = ConnectionState.STATE_DISCONNECTED
        completeDisconnect(status = GATT_SUCCESS)
    }

    private fun connectionStateChangeUnsuccessful(
        status: Int,
        newState: Int,
        timePassed: Long
    ) {
        // Service discovery is still pending so cancel it
        mainHandler.removeCallbacks(discoverServicesRunnable)
        val services = services
        val servicesDiscovered = services.isNotEmpty()

        // See if the initial connection failed
        if (state == ConnectionState.STATE_CONNECTING) {
            val isTimeout = timePassed > timeoutThreshold
            Log.i(
                TAG,
                "connection failed with status '${statusToString(status)}' (${if (isTimeout) "TIMEOUT" else "ERROR"})"
            )
            bleLogger(
                "connection failed with status '${statusToString(status)}' (${if (isTimeout) "TIMEOUT" else "ERROR"})",
                type = EventType.ERROR
            )
            val adjustedStatus =
                if (status == GATT_ERROR && isTimeout) GATT_CONN_TIMEOUT else status
            completeDisconnect(status = adjustedStatus)
        } else if (state == ConnectionState.STATE_CONNECTED && newState == BluetoothProfile.STATE_DISCONNECTED && !servicesDiscovered) {
            // We got a disconnection before the services were even discovered
            Log.i(
                TAG,
                "peripheral '$name' disconnected with status '${statusToString(status)}' before completing service discovery"
            )
            completeDisconnect(status = status)
        } else {
            // See if we got connection drop
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(
                    TAG,
                    "peripheral '$name' disconnected with status '${statusToString(status)}'"
                )
            } else {
                Log.i(
                    TAG,
                    "unexpected connection state change for '$name' status '${statusToString(status)}'"
                )
                bleLogger(
                    "unexpected connection state change for '$name' status '${
                        statusToString(
                            status
                        )
                    }'", type = EventType.WARNING
                )
            }
            completeDisconnect(status = status)
        }
    }

    private val bondStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            val action = intent.action ?: return
            val device =
                intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    ?: return

            // Ignore updates for other devices
            if (device.address != bluetoothGatt?.device?.address)
                return

            // Take action depending on new bond state
            if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                val bondState =
                    intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                val previousBondState =
                    intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                when (bondState) {
                    BOND_BONDING -> {
                        Log.d(
                            TAG,
                            "starting bonding with ${device.name}-${device.address}"
                        )
                        bleLogger(
                            "starting bonding with ${device.name}-${device.address}",
                            type = EventType.INFO
                        )
                        callbackHandler.post { peripheralCallback?.onBondingStarted() }
                    }
                    BOND_BONDED -> {
                        // Bonding succeeded
                        Log.d(TAG, "bonded with ${device.name}-${device.address}")
                        bleLogger(
                            "bonded with ${device.name}-${device.address}",
                            type = EventType.INFO
                        )
                        callbackHandler.post { peripheralCallback?.onBondingSucceeded() }

                        // If bonding was started at connection time, we may still have to discover the services
                        if (bluetoothGatt?.services?.isEmpty() == true) {
                            bleLogger(
                                "bonded, start discovering services",
                                type = EventType.INFO
                            )
                            delayedDiscoverServices(0)
                        }

                        // If bonding was triggered by a read/write, we must retry it
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                            if (commandStatus != null && !manuallyBonding) {
                                mainHandler.postDelayed({
                                    Log.d(TAG, "retrying command after bonding")
                                    bleLogger(
                                        "retrying command after bonding",
                                        type = EventType.INFO
                                    )
                                    retryCommand()
                                }, 50)
                            }
                        }

                        // If we are doing a manual bond, complete the command
                        if (manuallyBonding) {
                            manuallyBonding = false
                            completedCommand()
                        }
                    }
                    BOND_NONE -> {
                        if (previousBondState == BOND_BONDING) {
                            Log.e(TAG, "bonding failed for '$name', disconnecting device")
                            callbackHandler.post { peripheralCallback?.onBondingFailed() }
                        } else {
                            Log.e(TAG, "bond lost for '$name'")
                            bleLogger("bond lost for '$name'")
                            bondLost = true
                            // Cancel the discoverServiceRunnable if it is still pending
                            mainHandler.removeCallbacks(discoverServicesRunnable)
                            callbackHandler.post { peripheralCallback?.onBondLost() }
                        }
                        disconnect()
                    }
                }
            }
        }
    }
    private val pairingRequestBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            val device =
                intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    ?: return

            // Skip other devices
            if (device.address != bluetoothGatt?.device?.address)
                return

            // String values are used as the constants are not available for Android 4.3.
            val variant = intent.getIntExtra(
                "android.bluetooth.device.extra.PAIRING_VARIANT" /*BluetoothDevice.EXTRA_PAIRING_VARIANT*/,
                0
            )
            Log.d(
                TAG,
                "pairing request received, pairing variant: ${
                    pairingVariantToString(
                        variant
                    )
                } ($variant)"
            )
            bleLogger(
                " pairing request received, pairing variant: ${
                    pairingVariantToString(
                        variant
                    )
                } ($variant)\""
            )
        }
    }

    /**
     * Connect directly with the bluetooth device. This call will timeout in max 30 seconds (5 seconds on Samsung phones)
     */
    fun connect() {
        Log.i(TAG, "connect to '$name' ($address) - $state")
        // Make sure we are disconnected before we start making a connection
        if (state == ConnectionState.STATE_DISCONNECTED ||
            state == ConnectionState.STATE_DISCONNECTING ||
            state == ConnectionState.STATE_FAILED
        ) {
            // Register bonding broadcast receiver
            context.registerReceiver(
                bondStateReceiver,
                IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            )
            context.registerReceiver(
                pairingRequestBroadcastReceiver,
                IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST")
            )
            mainHandler.postDelayed({ // Connect to device with autoConnect = false
                Log.i(TAG, "connect to '$name' ($address) using TRANSPORT_LE")
                bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    device.connectGatt(
                        context,
                        false,
                        bluetoothGattCallback,
                        BluetoothDevice.TRANSPORT_LE
                    )
                } else {
                    // Versions below Nougat had a race condition bug in connect, so use special workaround
                    connectGattHelper(device, false, bluetoothGattCallback)
                }
                connectTimestamp = SystemClock.elapsedRealtime()
                startConnectionTimer()
                listener.connecting(peripheral = this)
                state = ConnectionState.STATE_CONNECTING
            }, DIRECT_CONNECTION_DELAY_IN_MS.toLong())
        } else {
            Log.i(TAG, "peripheral not disconnected, ignoring connect")
        }
    }

    /**
     * Create a bond with the peripheral.
     *
     *
     * If a (auto)connect has been issued, the bonding command will be enqueued and you will
     * receive updates via the [BluetoothPeripheralCallback]. Otherwise the bonding will
     * be done immediately and no updates via the callback will happen.
     *
     * @return true if bonding was started/enqueued, false if not
     */
    private fun createBond(): Boolean {
        // Check if we have a Gatt object
        if (bluetoothGatt == null) {
            // No gatt object so no connection issued, do create bond immediately
            return device.createBond()
        }

        // Enqueue the bond command because a connection has been issued or we are already connected
        val result = commandQueue.add(BTCommand({
            manuallyBonding = true
            if (!device.createBond()) {
                Log.e("bonding failed for %s", address)
                bleLogger("bonding failed for %s\", address", type = EventType.ERROR)
                completedCommand()
            } else {
                Log.d("manually bonding %s", address)
                bleLogger("manually bonding %s\", address", type = EventType.WARNING)
                nrTries++
            }
        }, null, CommandType.COMMAND_SET_BOND))
        if (result) {
            nextCommand()
        } else {
            Log.e(TAG, "could not enqueue bonding command")
            bleLogger("could not enqueue bonding command", type = EventType.ERROR)
        }
        return result
    }

    /**
     * Request a different connection priority.
     *
     *
     * Use the standard parameters for Android: CONNECTION_PRIORITY_BALANCED, CONNECTION_PRIORITY_HIGH, or CONNECTION_PRIORITY_LOW_POWER. There is no callback for this function.
     *
     * @param priority the requested connection priority
     * @return true if request was enqueued, false if not
     */
    @Suppress("unused")
    fun requestConnectionPriority(priority: Int): Boolean {
        // Enqueue the request connection priority command and complete is immediately as there is no callback for it
        val result = commandQueue.add(BTCommand({
            if (isConnected) {
                if (bluetoothGatt?.requestConnectionPriority(priority) == false) {
                    Log.e(TAG, "could not set connection priority")
                    bleLogger(
                        "could not set connection priority",
                        type = EventType.WARNING
                    )
                } else {
                    Log.d(TAG, "requesting connection priority $priority")
                    bleLogger(
                        "requesting connection priority $priority",
                        type = EventType.WARNING
                    )
                }
                completedCommand()
            }
        }, null, CommandType.COMMAND_GENERIC))
        if (result) {
            nextCommand()
        } else {
            Log.e(TAG, "could not enqueue request connection priority command")
            bleLogger(
                "could not enqueue request connection priority command",
                type = EventType.WARNING
            )
        }
        return result
    }

    /**
     * Version of createBond with transport parameter.
     * May use in the future if needed as I never encountered an issue
     */
    @Suppress("unused")
    private fun createBond(transport: Int): Boolean {
        Log.d(TAG, "bonding using TRANSPORT_LE")
        var result = false
        try {
            val bondMethod = device.javaClass.getMethod("createBond", Int::class.javaPrimitiveType)
            result = bondMethod.invoke(device, transport) as Boolean
            bleLogger("Creating Bond Request, Status-$result")
        } catch (e: Exception) {
            Log.e(TAG, "could not invoke createBond method")
            bleLogger("could not invoke createBond method", type = EventType.ERROR)
        }
        return result
    }

    /**
     * Disconnect the bluetooth peripheral.
     *
     *
     * When the disconnection has been completed [BluetoothCentralCallback.onDisconnectedPeripheral] will be called.
     */
    fun disconnect() {
        mainHandler.removeCallbacks(timeoutRunnable)
        if (state == ConnectionState.STATE_CONNECTED ||
            state == ConnectionState.STATE_CONNECTING
        ) {
            mainHandler.post {
                bluetoothGatt?.disconnect()
            }
            if (state == ConnectionState.STATE_CONNECTING) {
                // Since we will not get a callback on onConnectionStateChange for this,
                // we complete the disconnect ourselves
                mainHandler.postDelayed({ completeDisconnect(status = GATT_SUCCESS) }, 50)
            }
        } else {
            state = ConnectionState.STATE_DISCONNECTED
            listener.disconnected(this@BluetoothPeripheral, GATT_CONN_TERMINATE_LOCAL_HOST)
        }
    }

    fun disconnectWhenBluetoothOff() {
        completeDisconnect(status = GATT_SUCCESS)
    }

    /**
     * Complete the disconnect after getting connection_state = disconnected
     */
    private fun completeDisconnect(delay: Long = 0L, status: Int) {
        bluetoothGatt?.close()
        bluetoothGatt = null
        commandQueue.clear()
        commandStatus = null
        try {
            context.unregisterReceiver(bondStateReceiver)
            context.unregisterReceiver(pairingRequestBroadcastReceiver)
        } catch (e: Exception) {
        }
        bondLost = false
        state = ConnectionState.STATE_DISCONNECTED
        callbackHandler.postDelayed(
            {
                listener.disconnected(this@BluetoothPeripheral, status)
            },
            delay
        )
    }

    /**
     * Get the mac address of the bluetooth peripheral.
     *
     * @return Address of the bluetooth peripheral
     */
    val address: String
        get() = device.address

    /**
     * Get the type of the peripheral.
     *
     * @return the device type [.DEVICE_TYPE_CLASSIC], [.DEVICE_TYPE_LE] [.DEVICE_TYPE_DUAL]. [.DEVICE_TYPE_UNKNOWN] if it's not available
     */
    val type: Int
        get() = device.type// Cache the name so that we even know it when bluetooth is switched off

    /**
     * Get the name of the bluetooth peripheral.
     *
     * @return name of the bluetooth peripheral
     */
    val name: String?
        get() {
            val name = device.name
            if (name != null) {
                // Cache the name so that we even know it when bluetooth is switched off
                cachedName = name
            }
            return cachedName
        }

    /**
     * Get the bond state of the bluetooth peripheral.
     *
     *
     * Possible values for the bond state are:
     * [.BOND_NONE],
     * [.BOND_BONDING],
     * [.BOND_BONDED].
     *
     * @return returns the bond state
     */
    @Suppress("unused")
    val bondState: Int
        get() = device.bondState

    /**
     * Get the services supported by the connected bluetooth peripheral.
     * Only services that are also supported by [BluetoothCentral] are included.
     *
     * @return Supported services.
     */
    private val services: List<BluetoothGattService>
        get() = bluetoothGatt?.services ?: emptyList()

    /**
     * Get the BluetoothGattService object for a service UUID.
     *
     * @param serviceUUID the UUID of the service
     * @return the BluetoothGattService object for the service UUID or null if the peripheral does not have a service with the specified UUID
     */
    private fun getService(serviceUUID: UUID?): BluetoothGattService? {
        return bluetoothGatt?.getService(serviceUUID)
    }

    /**
     * Get the BluetoothGattCharacteristic object for a characteristic UUID.
     *
     * @param serviceUUID        the service UUID the characteristic is part of
     * @param characteristicUUID the UUID of the characteristic
     * @return the BluetoothGattCharacteristic object for the characteristic UUID or null if the peripheral does not have a characteristic with the specified UUID
     */
    @Suppress("unused")
    fun getCharacteristic(
        serviceUUID: UUID?,
        characteristicUUID: UUID?
    ): BluetoothGattCharacteristic? {
        val service = getService(serviceUUID)
        return service?.getCharacteristic(characteristicUUID)
    }

    /**
     * Boolean to indicate if the specified characteristic is currently notifying or indicating.
     *
     * @param characteristic the characteristic
     * @return true is the characteristic is notifying or indicating, false if it is not
     */
    fun isNotifying(characteristic: BluetoothGattCharacteristic): Boolean {
        return notifyingCharacteristics.contains(characteristic.uuid)
    }

    private val isConnected: Boolean
        get() = bluetoothGatt != null && state == ConnectionState.STATE_CONNECTED

    /**
     * Read the value of a characteristic.
     *
     *
     * The characteristic must support reading it, otherwise the operation will not be enqueued.
     *
     *
     * [BluetoothPeripheralCallback.onCharacteristicUpdate]   will be triggered as a result of this call.
     *
     * @param characteristic Specifies the characteristic to read.
     * @return true if the operation was enqueued, false if the characteristic does not support reading or the characteristic was invalid
     */
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic?): Boolean {
        // Check if gatt object is valid
        if (bluetoothGatt == null) {
            Log.e(TAG, "gatt is 'null', ignoring read request")
            bleLogger("gatt is 'null', ignoring read request", type = EventType.WARNING)
            return false
        }

        // Check if characteristic is valid
        if (characteristic == null) {
            Log.e(TAG, "characteristic is 'null', ignoring read request")
            bleLogger(
                "characteristic is 'null', ignoring read request",
                type = EventType.ERROR
            )
            return false
        }

        // Check if this characteristic actually has READ property
        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ == 0) {
            Log.e(TAG, "characteristic does not have read property")
            bleLogger(
                "characteristic does not have read property",
                type = EventType.ERROR
            )
            return false
        }

        // Enqueue the read command now that all checks have been passed
        val result = commandQueue.add(BTCommand({
            if (isConnected) {
                if (bluetoothGatt?.readCharacteristic(characteristic) == false) {
                    Log.e(
                        TAG,
                        "readCharacteristic failed for characteristic: ${characteristic.uuid}"
                    )
                    bleLogger(
                        "readCharacteristic failed for characteristic: ${characteristic.uuid}, value-${characteristic.value}",
                        type = EventType.ERROR
                    )
                    completedCommand()
                } else {
                    Log.d(TAG, "reading characteristic ${characteristic.uuid}")
                    nrTries++
                }
            } else {
                completedCommand()
            }
        }, null, CommandType.COMMAND_TO_READ))
        if (result) {
            nextCommand()
        } else {
            Log.e(TAG, "could not enqueue read characteristic command")
            bleLogger(
                "could not enqueue read characteristic command",
                type = EventType.ERROR
            )
        }
        return result
    }

    /**
     * Write a value to a characteristic using the specified write type.
     *
     *
     * All parameters must have a valid value in order for the operation
     * to be enqueued. If the characteristic does not support writing with the specified writeType, the operation will not be enqueued.
     *
     *
     * [BluetoothPeripheralCallback.onCharacteristicWrite] will be triggered as a result of this call.
     *
     * @param characteristic the characteristic to write to
     * @param value          the byte array to write
     * @param writeType      the write type to use when writing. Must be WRITE_TYPE_DEFAULT, WRITE_TYPE_NO_RESPONSE or WRITE_TYPE_SIGNED
     * @return true if a write operation was successfully enqueued, otherwise false
     */
    fun writeCharacteristic(
        characteristic: BluetoothGattCharacteristic?,
        value: ByteArray?,
        writeType: Int,
        responseNotified: Boolean,
        key: UUID?
    ): Boolean {

        // Check if gatt object is valid
        if (bluetoothGatt == null) {
            Log.e(TAG, "gatt is 'null', ignoring read request")
            bleLogger(
                "gatt is 'null', ignoring read request, value -${value ?: ""}",
                type = EventType.ERROR
            )
            return false
        }

        // Check if characteristic is valid
        if (characteristic == null) {
            Log.e(TAG, "characteristic is 'null', ignoring write request")
            bleLogger(
                "characteristic is 'null', ignoring write request, value -${value ?: ""}",
                type = EventType.ERROR
            )
            return false
        }

        // Check if byte array is valid
        if (value == null) {
            Log.e(TAG, "value to write is 'null', ignoring write request")
            bleLogger(
                "value to write is 'null', ignoring write request",
                type = EventType.ERROR
            )
            return false
        }

        // Copy the value to avoid race conditions
        val bytesToWrite = copyOf(value)

        // Check if this characteristic actually supports this writeType
        val writeProperty: Int = when (writeType) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT -> BluetoothGattCharacteristic.PROPERTY_WRITE
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE -> BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
            BluetoothGattCharacteristic.WRITE_TYPE_SIGNED -> BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE
            else -> 0
        }
        if (characteristic.properties and writeProperty == 0) {
            Log.e(
                TAG,
                "characteristic ${characteristic.uuid} does not support writeType ${
                    writeTypeToString(
                        writeType
                    )
                }"
            )
            bleLogger(
                " characteristic ${characteristic.uuid} does not support writeType ${
                    writeTypeToString(
                        writeType
                    )
                }", type = EventType.ERROR
            )
            return false
        }

        // Enqueue the write command now that all checks have been passed
        val result = commandQueue.add(
            BTCommand(
                {
                    if (isConnected) {
                        currentWriteBytes = bytesToWrite
                        characteristic.value = bytesToWrite
                        characteristic.writeType = writeType
                        if (bluetoothGatt?.writeCharacteristic(characteristic) == false) {
                            Log.e(
                                TAG,
                                "Tx failed: ${bytesToWrite.contentToString()}"
                            )
                            bleLogger(
                                "Tx failed: ${bytesToWrite.contentToString()}",
                                type = EventType.ERROR
                            )
                            peripheralCallback?.removeCommand(key, true)
                            handler.postDelayed({
                                completedCommand()
                            }, 2000)
                        } else {
                            Log.d(
                                TAG,
                                "writing ${bytesToWrite.toHexString()} to characteristic ${characteristic.uuid} posted"
                            )
                            try {
                                bleLogger(
                                    TAG.plus("Tx posted: ${bytesToWrite.toHexString()}"),
                                    type = EventType.INFO
                                )
                            } catch (e: Exception) {
                                /*Nothing do here.*/
                            }
                            nrTries++
                        }
                    } else {
                        completedCommand()
                    }
                },
                key,
                if (responseNotified) {
                    CommandType.COMMAND_TO_WRITE_WITH_NOTIFY_RESPONSE
                } else {
                    CommandType.COMMAND_TO_WRITE
                }
            )
        )
        if (result) {
            nextCommand()
        } else {
            Log.e(TAG, "could not enqueue write characteristic command")
            bleLogger(
                "Tx failed to enqueue: ${bytesToWrite.contentToString()}",
                type = EventType.ERROR
            )
        }
        return result
    }

    /**
     * Read the value of a descriptor.
     *
     * @param descriptor the descriptor to read
     * @return true if a write operation was successfully enqueued, otherwise false
     */
    fun readDescriptor(descriptor: BluetoothGattDescriptor?): Boolean {
        // Check if gatt object is valid
        if (bluetoothGatt == null) {
            Log.e(TAG, "gatt is 'null', ignoring read request")
            bleLogger(
                "gatt is 'null', ignoring read descriptor request",
                type = EventType.ERROR
            )
            return false
        }

        // Check if characteristic is valid
        if (descriptor == null) {
            Log.e(TAG, "descriptor is 'null', ignoring read request")
            bleLogger(
                "descriptor is 'null', ignoring read request",
                type = EventType.ERROR
            )
            return false
        }

        // Enqueue the read command now that all checks have been passed
        val result = commandQueue.add(BTCommand({
            // Double check if gatt is still valid
            if (isConnected) {
                if (bluetoothGatt?.readDescriptor(descriptor) == false) {
                    Log.e(
                        TAG,
                        "readDescriptor failed for characteristic: ${descriptor.uuid}"
                    )
                    bleLogger(
                        "readDescriptor failed for characteristic: ${descriptor.uuid}",
                        type = EventType.ERROR
                    )
                    completedCommand()
                } else {
                    nrTries++
                }
            } else {
                completedCommand()
            }
        }, null, CommandType.COMMAND_TO_READ))
        if (result) {
            nextCommand()
        } else {
            Log.e(TAG, "could not enqueue read descriptor command")
            bleLogger(
                "could not enqueue read descriptor command",
                type = EventType.ERROR
            )
        }
        return result
    }

    /**
     * Write a value to a descriptor.
     *
     *
     * For turning on/off notifications use [BluetoothPeripheral.setNotify] instead.
     *
     * @param descriptor the descriptor to write to
     * @param value      the value to write
     * @return true if a write operation was succesfully enqueued, otherwise false
     */
    fun writeDescriptor(
        descriptor: BluetoothGattDescriptor?,
        value: ByteArray?
    ): Boolean {
        // Check if gatt object is valid
        if (bluetoothGatt == null) {
            Log.e(TAG, "gatt is 'null', ignoring write descriptor request")
            bleLogger(
                "gatt is 'null', ignoring write descriptor request",
                type = EventType.ERROR
            )
            return false
        }

        // Check if characteristic is valid
        if (descriptor == null) {
            Log.e(TAG, "descriptor is 'null', ignoring write request")
            bleLogger(
                "descriptor is 'null', ignoring write request",
                type = EventType.ERROR
            )
            return false
        }

        // Check if byte array is valid
        if (value == null) {
            Log.e(TAG, "value to write is 'null', ignoring write request")
            bleLogger(
                "value to write is 'null', ignoring write request",
                type = EventType.ERROR
            )
            return false
        }

        // Copy the value to avoid race conditions
        val bytesToWrite = copyOf(value)

        // Enqueue the write command now that all checks have been passed
        val result = commandQueue.add(BTCommand({
            if (isConnected) {
                currentWriteBytes = bytesToWrite
                descriptor.value = bytesToWrite
                if (bluetoothGatt?.writeDescriptor(descriptor) == false) {
                    Log.e(TAG, "writeDescriptor failed for descriptor: ${descriptor.uuid}")
                    bleLogger(
                        "writeDescriptor failed for descriptor: ${descriptor.uuid} ,value= $value",
                        type = EventType.ERROR
                    )
                    completedCommand()
                } else {
                    Log.d(
                        TAG,
                        "writing ${bytesToWrite.contentToString()} to descriptor ${descriptor.uuid}"
                    )
                    nrTries++
                }
            } else {
                completedCommand()
            }
        }, null, CommandType.COMMAND_TO_WRITE))
        if (result) {
            nextCommand()
        } else {
            Log.e(TAG, "could not enqueue write descriptor command")
            bleLogger(
                "could not enqueue write descriptor command",
                type = EventType.ERROR
            )
        }
        return result
    }

    /**
     * Set the notification state of a characteristic to 'on' or 'off'. The characteristic must support notifications or indications.
     *
     *
     * [BluetoothPeripheralCallback.onNotificationStateUpdate] will be triggered as a result of this call.
     *
     * @param characteristic the characteristic to turn notification on/off for
     * @param enable         true for setting notification on, false for turning it off
     * @return true if the operation was enqueued, false if the characteristic doesn't support notification or indications or
     */
    fun setNotify(
        characteristic: BluetoothGattCharacteristic?,
        enable: Boolean
    ): Boolean {
        // Check if gatt object is valid
        if (bluetoothGatt == null) {
            Log.e(TAG, "gatt is 'null', ignoring set notify request")
            bleLogger(
                "gatt is 'null', ignoring set notify request",
                type = EventType.ERROR
            )
            return false
        }

        // Check if characteristic is valid
        if (characteristic == null) {
            Log.e(TAG, "characteristic is 'null', ignoring setNotify request")
            bleLogger(
                "characteristic is 'null', ignoring setNotify request",
                type = EventType.ERROR
            )
            return false
        }

        // Get the Client Configuration Descriptor for the characteristic
        val descriptor =
            characteristic.getDescriptor(UUID.fromString(CCC_DESCRIPTOR_UUID))
        if (descriptor == null) {
            Log.e(TAG, "could not get CCC descriptor for characteristic ${characteristic.uuid}")
            bleLogger(
                "could not get CCC descriptor for characteristic ${characteristic.uuid}",
                type = EventType.ERROR
            )
            return false
        }
        Log.d(TAG, "${characteristic.uuid}: notify $enable request sent. ${descriptor.uuid}")

        // Check if characteristic has NOTIFY or INDICATE properties and set the correct byte value to be written
        val value: ByteArray
        val properties = characteristic.properties
        value = when {
            properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0 -> {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }
            properties and BluetoothGattCharacteristic.PROPERTY_INDICATE > 0 -> {
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            }
            else -> {
                Log.e(
                    TAG,
                    "characteristic ${characteristic.uuid} does not have notify or indicate property"
                )
                return false
            }
        }
        val finalValue =
            if (enable) value else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        // Queue Runnable to turn on/off the notification now that all checks have been passed
        val result = commandQueue.add(BTCommand(Runnable {
            if (!isConnected) {
                completedCommand()
                return@Runnable
            }
            // First set notification for Gatt object
            if (bluetoothGatt?.setCharacteristicNotification(
                    descriptor.characteristic,
                    enable
                ) == false
            ) {
                Log.e(
                    TAG,
                    "setCharacteristicNotification failed for characteristic: ${descriptor.characteristic.uuid}"
                )
                bleLogger(
                    "setCharacteristicNotification failed for characteristic: ${descriptor.characteristic.uuid}",
                    type = EventType.ERROR
                )
            }
            // Then write to descriptor
            currentWriteBytes = finalValue
            descriptor.value = finalValue
            val result: Boolean
            Thread.sleep(100)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                result = bluetoothGatt?.writeDescriptor(descriptor) ?: false
            } else {
                // Up to Android 6 there is a bug where Android takes the writeType of the parent characteristic instead of always WRITE_TYPE_DEFAULT
                // See: https://android.googlesource.com/platform/frameworks/base/+/942aebc95924ab1e7ea1e92aaf4e7fc45f695a6c%5E%21/#F0
                val parentCharacteristic = descriptor.characteristic
                val originalWriteType = parentCharacteristic.writeType
                parentCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                result = bluetoothGatt?.writeDescriptor(descriptor) ?: false
                parentCharacteristic.writeType = originalWriteType
            }
            if (!result) {
                completedCommand()
            } else {
                nrTries++
            }
        }, null, CommandType.COMMAND_SET_NOTIFY))
        if (result) {
            nextCommand()
        } else {
            Log.e(TAG, "could not enqueue setNotify command")
        }
        return result
    }

    /**
     * Asynchronous method to clear the services cache. Make sure to add a delay when using this!
     *
     * @return true if the method was executed, false if not executed
     */
    @Suppress("unused")
    fun clearServicesCache(): Boolean {
        var result = false
        try {
            val refreshMethod =
                bluetoothGatt?.javaClass?.getMethod("refresh")
            if (refreshMethod != null) {
                result = refreshMethod.invoke(bluetoothGatt) as Boolean
            }
        } catch (e: Exception) {
            Log.e(TAG, "could not invoke refresh method")
        }
        return result
    }

    /**
     * Read the RSSI for a connected remote peripheral.
     *
     *
     * [BluetoothPeripheralCallback.onReadRemoteRssi] will be triggered as a result of this call.
     *
     * @return true if the operation was enqueued, false otherwise
     */
    fun readRemoteRssi(): Boolean {
        val result = commandQueue.add(BTCommand({
            if (isConnected) {
                if (bluetoothGatt?.readRemoteRssi() == false) {
                    Log.e(TAG, "readRemoteRssi failed")
                    completedCommand()
                }
            } else {
                Log.e(TAG, "cannot get rssi, peripheral not connected")
                completedCommand()
            }
        }, null, CommandType.COMMAND_TO_WRITE))
        if (result) {
            nextCommand()
        } else {
            Log.e(TAG, "could not enqueue setNotify command")
        }
        return result
    }

    /**
     * Request an MTU size used for a given connection.
     *
     *
     * When performing a write request operation (write without response),
     * the data sent is truncated to the MTU size. This function may be used
     * to request a larger MTU size to be able to send more data at once.
     *
     *
     * [BluetoothPeripheralCallback.onMtuChanged] will be triggered as a result of this call.
     *
     * @param mtu the desired MTU size
     * @return true if the operation was enqueued, false otherwise
     */
    fun requestMtu(mtu: Int): Boolean {
        val result = commandQueue.add(BTCommand({
            if (isConnected) {
                if (bluetoothGatt?.requestMtu(mtu) == false) {
                    Log.e(TAG, "requestMtu failed")
                    bleLogger(TAG.plus(" requestMtu failed"), type = EventType.ERROR)
                    completedCommand()
                }
            } else {
                Log.e(TAG, "cannot request MTU, peripheral not connected")
                bleLogger(
                    TAG.plus(" cannot request MTU, peripheral not connected"),
                    type = EventType.ERROR
                )
                completedCommand()
            }
        }, null, CommandType.COMMAND_TO_WRITE))
        if (result) {
            nextCommand()
        } else {
            Log.e(TAG, "could not enqueue setNotify command")
            bleLogger(TAG.plus(" could not enqueue setNotify command"), type = EventType.ERROR)

        }
        return result
    }

    /**
     * The current command has been completed, move to the next command in the queue (if any)
     */
    private fun completedCommand() {
        Log.i(
            TAG,
            "completedCommand : ${commandStatus?.commandState} - ${commandStatus?.commandType}"
        )
        if (commandStatus?.commandState?.value == 3) {
            peripheralCallback?.removeCommand(commandStatus?.key)
            commandQueue.poll()
            commandStatus = null
            nextCommand()
        } else if (commandStatus == null) {
            nextCommand()
        }
    }

    /**
     * Retry the current command. Typically used when a read/write fails and triggers a bonding procedure
     */
    private fun retryCommand() {
        val currentCommand = commandQueue.peek()
        if (currentCommand != null) {
            if (nrTries >= MAX_TRIES) {
                // Max retries reached, give up on this one and proceed
                Log.d(TAG, "max number of tries reached, not retrying operation anymore")
                bleLogger(
                    TAG.plus(" max number of tries reached, not retrying operation anymore"),
                    type = EventType.ERROR
                )
                commandQueue.poll()
            } else {
                commandStatus?.commandState = CommandState.COMMAND_IN_RETRY
            }
        }
        nextCommand()
    }

    /**
     * Re execute the current command.
     */
    private fun reExecuteCommand() {
        commandStatus?.commandState = CommandState.COMMAND_IN_PROGRESS
        val currentCommand = commandQueue.peek()
        if (currentCommand != null) {
            nextCommand()
        }
    }

    /**
     * Execute the next command in the subscribe queue.
     * A queue is used because the calls have to be executed sequentially.
     * If the read or write fails, the next command in the queue is executed.
     */
    private fun nextCommand() {
        // Make sure only one thread can execute this method
        synchronized(this) {
            // If there is still a command being executed then bail out
            if (commandStatus != null) {
                if (commandStatus?.commandState != CommandState.COMMAND_IN_RETRY) {
                    return
                }
            } else {
                handler.removeCallbacks(commandTimeoutRunnable)
            }

            // Check if we still have a valid gatt object
            if (bluetoothGatt == null) {
                Log.e(TAG, "gatt is 'null' for peripheral '$address', clearing command queue")
                bleLogger(
                    "gatt is 'null' for peripheral '$address', clearing command queue",
                    type = EventType.ERROR
                )
                commandQueue.clear()
                return
            }

            // Execute the next command in the queue
            val bluetoothCommand = commandQueue.peek()
            if (bluetoothCommand != null) {
                if (commandStatus == null) {
                    nrTries = 0
                }

                commandStatus = CommandStatus(
                    bluetoothCommand.key,
                    bluetoothCommand.responseState,
                    CommandState.COMMAND_IN_PROGRESS
                )
                commandTimeout(5000L)
                mainHandler.post {
                    try {
                        bluetoothCommand.runnable.run()
                    } catch (ex: Exception) {
                        Log.e(TAG, "command exception for device '$name' - $ex")
                        bleLogger(
                            "command exception for device '$name' - $ex",
                            type = EventType.WARNING
                        )
                        completedCommand()
                    }
                }
            }
        }
    }

    private fun commandTimeout(timeout: Long = 2000) {
        handler.removeCallbacks(commandTimeoutRunnable)
        handler.postDelayed(commandTimeoutRunnable, timeout)
    }

    private fun bondStateToString(state: Int): String {
        return when (state) {
            BOND_NONE -> "BOND_NONE"
            BOND_BONDING -> "BOND_BONDING"
            BOND_BONDED -> "BOND_BONDED"
            else -> "UNKNOWN"
        }
    }

    /**
     * Converts the connection state to String value
     *
     * @param state the connection state
     * @return state as String
     */
    @Suppress("unused")
    private fun stateToString(state: Int): String {
        return when (state) {
            BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
            BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
            BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
            else -> "DISCONNECTED"
        }
    }

    private fun writeTypeToString(writeType: Int): String {
        return when (writeType) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT -> "WRITE_TYPE_DEFAULT"
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE -> "WRITE_TYPE_NO_RESPONSE"
            BluetoothGattCharacteristic.WRITE_TYPE_SIGNED -> "WRITE_TYPE_SIGNED"
            else -> "unknown writeType"
        }
    }

    private fun pairingVariantToString(variant: Int): String {
        return when (variant) {
            PAIRING_VARIANT_PIN -> "PAIRING_VARIANT_PIN"
            PAIRING_VARIANT_PASSKEY -> "PAIRING_VARIANT_PASSKEY"
            PAIRING_VARIANT_PASSKEY_CONFIRMATION -> "PAIRING_VARIANT_PASSKEY_CONFIRMATION"
            PAIRING_VARIANT_CONSENT -> "PAIRING_VARIANT_CONSENT"
            PAIRING_VARIANT_DISPLAY_PASSKEY -> "PAIRING_VARIANT_DISPLAY_PASSKEY"
            PAIRING_VARIANT_DISPLAY_PIN -> "PAIRING_VARIANT_DISPLAY_PIN"
            PAIRING_VARIANT_OOB_CONSENT -> "PAIRING_VARIANT_OOB_CONSENT"
            else -> "UNKNOWN"
        }
    }

    interface InternalCallback {
        /**
         * [BluetoothPeripheral] initiated connection.
         *
         * @param peripheral [BluetoothPeripheral] initiated connection.
         */
        fun connecting(peripheral: BluetoothPeripheral)

        /**
         * [BluetoothPeripheral] has successfully connected.
         *
         * @param peripheral [BluetoothPeripheral] that connected.
         */
        fun connected(peripheral: BluetoothPeripheral)

        /**
         * [BluetoothPeripheral] has disconnected.
         *
         * @param peripheral [BluetoothPeripheral] that disconnected.
         */
        fun disconnected(peripheral: BluetoothPeripheral, status: Int)

        /**
         * [BluetoothPeripheral] has started to bond.
         */
        fun bondingStarted()

        /**
         * [BluetoothPeripheral] is ready to work.
         *
         * @param peripheral [BluetoothPeripheral] that disconnected.
         */
        fun ready(peripheral: BluetoothPeripheral)

        /**
         * [BluetoothPeripheral] is ready to work.
         *
         * @param peripheral [BluetoothPeripheral] that disconnected.
         */
        fun notifyFailed(peripheral: BluetoothPeripheral)
    }

    private fun connectGattHelper(
        remoteDevice: BluetoothDevice,
        autoConnect: Boolean = true,
        bluetoothGattCallback: BluetoothGattCallback
    ): BluetoothGatt? {
        /*
          This bug workaround was taken from the Polidea RxAndroidBle
          Issue that caused a race condition mentioned below was fixed in 7.0.0_r1
          https://android.googlesource.com/platform/frameworks/base/+/android-7.0.0_r1/core/java/android/bluetooth/BluetoothGatt.java#649
          compared to
          https://android.googlesource.com/platform/frameworks/base/+/android-6.0.1_r72/core/java/android/bluetooth/BluetoothGatt.java#739
          issue: https://android.googlesource.com/platform/frameworks/base/+/d35167adcaa40cb54df8e392379dfdfe98bcdba2%5E%21/#F0
          */return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || !autoConnect) {
            connectGattCompat(bluetoothGattCallback, remoteDevice, autoConnect)
        } else try {
            val iBluetoothGatt = getIBluetoothGatt(iBluetoothManager)
            if (iBluetoothGatt == null) {
                Log.e(TAG, "could not get iBluetoothGatt object")
                return connectGattCompat(bluetoothGattCallback, remoteDevice, true)
            }
            val bluetoothGatt = createBluetoothGatt(iBluetoothGatt, remoteDevice)
            val connectedSuccessfully =
                connectUsingReflection(bluetoothGatt, bluetoothGattCallback)
            if (!connectedSuccessfully) {
                Log.i(TAG, "connection using reflection failed, closing gatt")
                bluetoothGatt.close()
            }
            bluetoothGatt
        } catch (exception: NoSuchMethodException) {
            Log.e(TAG, "error during reflection")
            connectGattCompat(bluetoothGattCallback, remoteDevice, true)
        } catch (exception: IllegalAccessException) {
            Log.e(TAG, "error during reflection")
            connectGattCompat(bluetoothGattCallback, remoteDevice, true)
        } catch (exception: IllegalArgumentException) {
            Log.e(TAG, "error during reflection")
            connectGattCompat(bluetoothGattCallback, remoteDevice, true)
        } catch (exception: InvocationTargetException) {
            Log.e(TAG, "error during reflection")
            connectGattCompat(bluetoothGattCallback, remoteDevice, true)
        } catch (exception: InstantiationException) {
            Log.e(TAG, "error during reflection")
            connectGattCompat(bluetoothGattCallback, remoteDevice, true)
        } catch (exception: NoSuchFieldException) {
            Log.e(TAG, "error during reflection")
            connectGattCompat(bluetoothGattCallback, remoteDevice, true)
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun connectGattCompat(
        bluetoothGattCallback: BluetoothGattCallback,
        device: BluetoothDevice,
        autoConnect: Boolean
    ): BluetoothGatt? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return device.connectGatt(
                context,
                autoConnect,
                bluetoothGattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Try to call connectGatt with TRANSPORT_LE parameter using reflection
            try {
                val connectGattMethod = device.javaClass.getMethod(
                    "connectGatt",
                    Context::class.java,
                    Boolean::class.javaPrimitiveType,
                    BluetoothGattCallback::class.java,
                    Int::class.javaPrimitiveType
                )
                try {
                    return connectGattMethod.invoke(
                        device,
                        context,
                        autoConnect,
                        bluetoothGattCallback,
                        BluetoothDevice.TRANSPORT_LE
                    ) as BluetoothGatt
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                }
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
            }
        }
        // Fallback on connectGatt without TRANSPORT_LE parameter
        return device.connectGatt(context, autoConnect, bluetoothGattCallback)
    }

    @Throws(
        NoSuchMethodException::class,
        InvocationTargetException::class,
        IllegalAccessException::class,
        NoSuchFieldException::class
    )
    private fun connectUsingReflection(
        bluetoothGatt: BluetoothGatt,
        bluetoothGattCallback: BluetoothGattCallback,
        autoConnect: Boolean = true
    ): Boolean {
        setAutoConnectValue(bluetoothGatt, autoConnect)
        val connectMethod = bluetoothGatt.javaClass.getDeclaredMethod(
            "connect",
            Boolean::class.java,
            BluetoothGattCallback::class.java
        )
        connectMethod.isAccessible = true
        return connectMethod.invoke(bluetoothGatt, true, bluetoothGattCallback) as Boolean
    }

    @Throws(
        IllegalAccessException::class,
        InvocationTargetException::class,
        InstantiationException::class
    )
    private fun createBluetoothGatt(
        iBluetoothGatt: Any,
        remoteDevice: BluetoothDevice
    ): BluetoothGatt {
        val bluetoothGattConstructor =
            BluetoothGatt::class.java.declaredConstructors[0]
        bluetoothGattConstructor.isAccessible = true
        return if (bluetoothGattConstructor.parameterTypes.size == 4) {
            bluetoothGattConstructor.newInstance(
                context,
                iBluetoothGatt,
                remoteDevice,
                BluetoothDevice.TRANSPORT_LE
            ) as BluetoothGatt
        } else {
            bluetoothGattConstructor.newInstance(
                context,
                iBluetoothGatt,
                remoteDevice
            ) as BluetoothGatt
        }
    }

    @Throws(
        NoSuchMethodException::class,
        InvocationTargetException::class,
        IllegalAccessException::class
    )
    private fun getIBluetoothGatt(iBluetoothManager: Any?): Any? {
        if (iBluetoothManager == null) {
            return null
        }
        val getBluetoothGattMethod =
            getMethodFromClass(iBluetoothManager.javaClass, "getBluetoothGatt")
        return getBluetoothGattMethod.invoke(iBluetoothManager)
    }

    @get:Throws(
        NoSuchMethodException::class,
        InvocationTargetException::class,
        IllegalAccessException::class
    )
    private val iBluetoothManager: Any?
        get() {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: return null
            val getBluetoothManagerMethod =
                getMethodFromClass(bluetoothAdapter.javaClass, "getBluetoothManager")
            return getBluetoothManagerMethod.invoke(bluetoothAdapter)
        }

    @Throws(NoSuchMethodException::class)
    private fun getMethodFromClass(
        cls: Class<*>,
        methodName: String
    ): Method {
        val method = cls.getDeclaredMethod(methodName)
        method.isAccessible = true
        return method
    }

    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    private fun setAutoConnectValue(
        bluetoothGatt: BluetoothGatt,
        autoConnect: Boolean
    ) {
        val autoConnectField =
            bluetoothGatt.javaClass.getDeclaredField("mAutoConnect")
        autoConnectField.isAccessible = true
        autoConnectField.setBoolean(bluetoothGatt, autoConnect)
    }

    private fun startConnectionTimer() {
        mainHandler.removeCallbacks(timeoutRunnable)
        mainHandler.postDelayed(timeoutRunnable, CONNECTION_TIMEOUT_IN_MS.toLong())
    }

    private val timeoutThreshold: Int
        get() {
            val manufacturer = Build.MANUFACTURER
            return if (manufacturer == "samsung") {
                TIMEOUT_THRESHOLD_SAMSUNG
            } else {
                TIMEOUT_THRESHOLD_DEFAULT
            }
        }

    private fun copyOf(source: ByteArray): ByteArray {
        val sourceLength = source.size
        val copy = ByteArray(sourceLength)
        System.arraycopy(source, 0, copy, 0, sourceLength)
        return copy
    }

    private fun enableNotifications() {
        handler.removeCallbacks(notificationEnableRunnable)
        if (pendingNotifyCharacteristics.isNotEmpty()) {
            pendingNotifyCharacteristics.firstOrNull()?.let {
                setNotify(it, true)
                handler.postDelayed(notificationEnableRunnable, 30000)
            }
        }
    }

    companion object {
        // CCC descriptor UUID
        private const val CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"
        // Gatt status values taken from Android source code:
        // https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/android-4.4.4_r2.0.1/stack/include/gatt_api.h
        /**
         * A GATT operation completed successfully
         */
        const val GATT_SUCCESS = 0

        /**
         * The connection was terminated because of a L2C failure
         */
        const val GATT_CONN_L2C_FAILURE = 1

        /**
         * The connection has timed out
         */
        const val GATT_CONN_TIMEOUT = 8

        /**
         * GATT read operation is not permitted
         */
        const val GATT_READ_NOT_PERMITTED = 2

        /**
         * GATT write operation is not permitted
         */
        const val GATT_WRITE_NOT_PERMITTED = 3

        /**
         * Insufficient authentication for a given operation
         */
        const val GATT_INSUFFICIENT_AUTHENTICATION = 5

        /**
         * The given request is not supported
         */
        const val GATT_REQUEST_NOT_SUPPORTED = 6

        /**
         * Insufficient encryption for a given operation
         */
        const val GATT_INSUFFICIENT_ENCRYPTION = 15

        /**
         * The connection was terminated by the peripheral
         */
        const val GATT_CONN_TERMINATE_PEER_USER = 19

        /**
         * The connection was terminated by the local host
         */
        const val GATT_CONN_TERMINATE_LOCAL_HOST = 22

        /**
         * The connection lost because of LMP timeout
         */
        const val GATT_CONN_LMP_TIMEOUT = 34

        /**
         * The connection was terminated due to MIC failure
         */
        const val BLE_HCI_CONN_TERMINATED_DUE_TO_MIC_FAILURE = 61

        /**
         * The connection cannot be established
         */
        const val GATT_CONN_FAIL_ESTABLISH = 62

        /**
         * The peripheral has no resources to complete the request
         */
        const val GATT_NO_RESOURCES = 128

        /**
         * Something went wrong in the bluetooth stack
         */
        const val GATT_INTERNAL_ERROR = 129

        /**
         * The GATT operation could not be executed because the stack is busy
         */
        const val GATT_BUSY = 132

        /**
         * Generic error, could be anything
         */
        const val GATT_ERROR = 133

        /**
         * Authentication failed
         */
        const val GATT_AUTH_FAIL = 137

        /**
         * The connection was cancelled
         */
        const val GATT_CONN_CANCEL = 256

        /**
         * Bluetooth device type, Unknown
         */
        const val DEVICE_TYPE_UNKNOWN = 0

        /**
         * Bluetooth device type, Classic - BR/EDR devices
         */
        const val DEVICE_TYPE_CLASSIC = 1

        /**
         * Bluetooth device type, Low Energy - LE-only
         */
        const val DEVICE_TYPE_LE = 2

        /**
         * Bluetooth device type, Dual Mode - BR/EDR/LE
         */
        const val DEVICE_TYPE_DUAL = 3

        /**
         * Indicates the remote device is not bonded (paired).
         *
         * There is no shared link key with the remote device, so communication
         * (if it is allowed at all) will be unauthenticated and unencrypted.
         */
        const val BOND_NONE = 10

        /**
         * Indicates bonding (pairing) is in progress with the remote device.
         */
        const val BOND_BONDING = 11

        /**
         * Indicates the remote device is bonded (paired).
         *
         * A shared link keys exists locally for the remote device, so
         * communication can be authenticated and encrypted.
         *
         * *Being bonded (paired) with a remote device does not necessarily
         * mean the device is currently connected. It just means that the pending
         * procedure was completed at some earlier time, and the link key is still
         * stored locally, ready to use on the next connection.
         * *
         */
        const val BOND_BONDED = 12

        // Maximum number of retries of commands
        private const val MAX_TRIES = 2

        // Delay to use when doing a connect
        private const val DIRECT_CONNECTION_DELAY_IN_MS = 100

        // Timeout to use if no callback on onConnectionStateChange happens
        private const val CONNECTION_TIMEOUT_IN_MS = 35000

        // Samsung phones time out after 5 seconds while most other phone time out after 30 seconds
        private const val TIMEOUT_THRESHOLD_SAMSUNG = 4500

        // Most other phone time out after 30 seconds
        private const val TIMEOUT_THRESHOLD_DEFAULT = 25000

        // When a bond is lost, the bluetooth stack needs some time to update its internal state
        private const val DELAY_AFTER_BOND_LOST = 1000L

        // The maximum number of enabled notifications Android supports (BT_GATT_NOTIFY_REG_MAX)
        private const val MAX_NOTIFYING_CHARACTERISTICS = 15
        private fun statusToString(error: Int): String {
            return when (error) {
                GATT_SUCCESS -> "SUCCESS"
                GATT_CONN_L2C_FAILURE -> "GATT CONN L2C FAILURE"
                GATT_CONN_TIMEOUT -> "GATT CONN TIMEOUT" // Connection timed out
                GATT_CONN_TERMINATE_PEER_USER -> "GATT CONN TERMINATE PEER USER"
                GATT_CONN_TERMINATE_LOCAL_HOST -> "GATT CONN TERMINATE LOCAL HOST"
                BLE_HCI_CONN_TERMINATED_DUE_TO_MIC_FAILURE -> "BLE_HCI_CONN_TERMINATED_DUE_TO_MIC_FAILURE"
                GATT_CONN_FAIL_ESTABLISH -> "GATT CONN FAIL ESTABLISH"
                GATT_CONN_LMP_TIMEOUT -> "GATT CONN LMP TIMEOUT"
                GATT_CONN_CANCEL -> "GATT CONN CANCEL "
                GATT_BUSY -> "GATT BUSY"
                GATT_ERROR -> "GATT ERROR" // Device not reachable
                GATT_AUTH_FAIL -> "GATT AUTH FAIL" // Device needs to be bonded
                GATT_NO_RESOURCES -> "GATT_NO_RESOURCES"
                else -> "UNKNOWN ($error)"
            }
        }

        private const val PAIRING_VARIANT_PIN = 0
        private const val PAIRING_VARIANT_PASSKEY = 1
        private const val PAIRING_VARIANT_PASSKEY_CONFIRMATION = 2
        private const val PAIRING_VARIANT_CONSENT = 3
        private const val PAIRING_VARIANT_DISPLAY_PASSKEY = 4
        private const val PAIRING_VARIANT_DISPLAY_PIN = 5
        private const val PAIRING_VARIANT_OOB_CONSENT = 6
    }
}

fun ByteArray.toHexString() = joinToString(", ") { "%02X".format(it) }