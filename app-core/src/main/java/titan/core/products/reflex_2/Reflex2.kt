package titan.core.products.reflex_2

import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.titan.logger.coreLogger
import titan.bluetooth.CommandResponseState
import titan.bluetooth.ProductInfo
import titan.core.*
import titan.core.bluetooth.ReflexProducts
import titan.core.products.*
import titan.core.products.reflex_3.R3MusicEventControlCommand
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

const val R2_COMMUNICATION_SERVICE = "d0a2ff00-2996-d38b-e214-86515df5a1df"

const val R2_COMMUNICATION_WRITE_CHAR = "7905ff01-b5ce-4e99-a40f-4b1e122d00d0"
const val R2_COMMUNICATION_READ_CHAR = "7905ff02-b5ce-4e99-a40f-4b1e122d00d0"
const val R2_REAL_TIME_READ_CHAR = "7905ff04-b5ce-4e99-a40f-4b1e122d00d0"

class Reflex2 internal constructor(private val context: Context) : TitanDevice {
    override fun dataReceived(
        value: ByteArray,
        characteristic: BluetoothGattCharacteristic,
        status: Int,
        notified: Boolean,
        key: UUID?
    ): CommandResponseState {
        var response: ResponseStatus? = null
        key?.let { uuid ->
            Commands.getInstance().get(uuid)?.let { command ->
                response = processCommand(command, value)
                if (response == ResponseStatus.INCOMPLETE) {
                    return CommandResponseState.INCOMPLETE
                } else if (response == ResponseStatus.COMPLETED) {
                    return CommandResponseState.COMPLETED
                }
            }
        }
        if (response != null && response != ResponseStatus.INCOMPATIBLE && key != null) {
            Commands.getInstance().remove(key)
        } else {
            if (characteristic.uuid.toString().equals(R2_REAL_TIME_READ_CHAR, true)) {
                if (R2FindPhoneCommand().check(byteArray = value) == ResponseStatus.COMPLETED) {
                    val intent = Intent(FIND_PHONE_START)
                    intent.putExtra("find", "find")
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    return CommandResponseState.COMPLETED
                }
                if (R2CameraCommand().check(byteArray = value) == ResponseStatus.COMPLETED) {
                    coreLogger("Camera Command: ${CameraMode.getMode(value[4].asInt()).name}")
                    val intent = Intent(ACTION_CAMERA_STATE)
                    intent.putExtra(EXTRA_CAMERA_STATE, CameraMode.getMode(value[4].asInt()).name)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    return CommandResponseState.COMPLETED
                }
                if (R2MusicEventControlCommand().checkEventControl(byteArray = value) == ResponseStatus.COMPLETED) {
                    coreLogger("Music Command :${MusicState.getMusicModeForReflex2(value[4].asInt()).name}")
                    val intent = Intent(ACTION_MUSIC_STATE)
                    intent.putExtra(
                        EXTRA_MUSIC_STATE,
                        MusicState.getMusicModeForReflex2(value[4].asInt())
                    )
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    return CommandResponseState.INCOMPATIBLE
                }
                if (R2CallReminderCommand().checkEventControl(byteArray = value) == ResponseStatus.COMPLETED){
                    coreLogger("Reflex2: Call End Command :${HandleCallState.getCallStateForReflex2(value[4].asInt())}")
                    val intent = Intent(ACTION_HANDLE_CALL_STATE)
                    intent.putExtra(
                            EXTRA_HANDLE_CALL_STATE,
                            HandleCallState.getCallStateForReflex2(value[4].asInt())
                    )
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    return CommandResponseState.INCOMPATIBLE
                }
            } else if (characteristic.uuid.toString().equals(R2_COMMUNICATION_READ_CHAR, true)) {
                if (R2CameraCommand().check(byteArray = value) == ResponseStatus.COMPLETED) {
                    val intent = Intent(ACTION_CAMERA_STATE)
                    intent.putExtra(EXTRA_CAMERA_STATE, CameraMode.getMode(value[4].asInt()).name)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    return CommandResponseState.COMPLETED
                }
            }
        }
        return CommandResponseState.INCOMPATIBLE
    }

    override fun failed(key: UUID?) {
        key?.let {
            Commands.getInstance().get(it)?.let { command ->
                failedCommand(command)
            }
        }
    }

    private fun failedCommand(command: DataCommand) {
        when (command) {
            is R2TimeCommand -> command.failed()
            is R2StepsSleepCommand -> command.failed()
            is R2BaseTimeCommand -> command.failed()
            is R2UserInfoCommand -> command.failed()
            is R2AutoSleepCommand -> command.failed()
            is R2AntiLostCommand -> command.failed()
            is R2AutoHeartRateCommand -> command.failed()
            is R2HistoricalHeartRateDatesCommand -> command.failed()
            is R2HeartRateCommand -> command.failed()
            is R2StepsAndSleepHistoricalDatesCommand -> command.failed()
            is R2DisplayInterfaceCommand -> command.failed()
            is R2SedentaryReminderCommand -> command.failed()
            is R2WristSelectionCommand -> command.failed()
            is R2AutoHRTestScheduleCommand -> command.failed()
            is R2DeviceInfoCommand -> command.failed()
            is R2FindPhoneCommand -> command.failed()
            is R2NotificationCommand -> command.failed()
            is R2CameraCommand -> command.failed()
            is R2LiftWristToViewCommand -> command.failed()
            is R2MultiMediaDisplayOnOrOffCommand -> command.failed()
            is R2FactoryResetCommand -> command.failed()
            is R2AlarmCommand -> command.failed()
            is R2FindBandCommand -> command.failed()
            is R2SetDeviceInfoCommand -> command.failed()
            is R2CallReminderCommand -> command.failed()
            is R2EnableRealTimeTransmissionCommand -> command.failed()
        }
    }

    private fun processCommand(command: DataCommand, value: ByteArray): ResponseStatus {
        return when (command) {
            is R2TimeCommand -> command.check(byteArray = value)
            is R2StepsSleepCommand -> command.check(packet = value)
            is R2BaseTimeCommand -> command.check(byteArray = value)
            is R2UserInfoCommand -> command.check(packet = value)
            is R2AutoSleepCommand -> command.check(byteArray = value)
            is R2AntiLostCommand -> command.check(byteArray = value)
            is R2AutoHeartRateCommand -> command.check(byteArray = value)
            is R2HistoricalHeartRateDatesCommand -> command.check(packet = value)
            is R2HeartRateCommand -> command.check(packet = value)
            is R2StepsAndSleepHistoricalDatesCommand -> command.check(packet = value)
            is R2DisplayInterfaceCommand -> command.check(byteArray = value)
            is R2SedentaryReminderCommand -> command.check(byteArray = value)
            is R2WristSelectionCommand -> command.check(byteArray = value)
            is R2AutoHRTestScheduleCommand -> command.check(byteArray = value)
            is R2DeviceInfoCommand -> command.check(byteArray = value)
            is R2FindPhoneCommand -> command.check(byteArray = value)
            is R2NotificationCommand -> command.check(byteArray = value)
            is R2CameraCommand -> command.check(byteArray = value)
            is R2LiftWristToViewCommand -> command.check(byteArray = value)
            is R2MultiMediaDisplayOnOrOffCommand -> command.check(byteArray = value)
            is R2FactoryResetCommand -> command.check(byteArray = value)
            is R2AlarmCommand -> command.check(byteArray = value)
            is R2FindBandCommand -> command.check(byteArray = value)
            is R2SetDeviceInfoCommand -> command.check(byteArray = value)
            is R2CallReminderCommand -> command.check(byteArray = value)
            is R2EnableRealTimeTransmissionCommand -> command.check(byteArray = value)
            else -> ResponseStatus.INCOMPATIBLE
        }
    }

    companion object {
        private val products = arrayListOf(
            ProductInfo(1, ReflexProducts.REFLEX_1.code),
            ProductInfo(2, ReflexProducts.REFLEX_2.code),
            ProductInfo(3, ReflexProducts.REFLEX_BEAT.code),
            ProductInfo(3, ReflexProducts.REFLEX_2C.code)
        )

        fun getProductInfo(code: String): ProductInfo {
            return when (ReflexProducts.getProduct(code)) {
                ReflexProducts.REFLEX_1 -> products[0]
                ReflexProducts.REFLEX_2 -> products[1]
                ReflexProducts.REFLEX_BEAT -> products[2]
                ReflexProducts.REFLEX_2C -> products[3]
                else -> throw Exception("$code is not supported")
            }
        }

        fun getSMSAppID(): Int {
            return 18
        }

        fun getDefaultNotifications() : Map<Pair<String, Int>, Array<String>>{
            val map = java.util.HashMap<Pair<String, Int>, Array<String>>()
            map[Pair("Call", 1)] = arrayOf("Call")
            map[Pair("SMS", 18)] = arrayOf("SMS")
            return map
        }

        fun supportedAppsForNotifications(): Map<Pair<String, Int>, Array<String>> {
            val map = HashMap<Pair<String, Int>, Array<String>>()
//            map[Pair("SMS", 18)] = arrayOf()
            map[Pair("Skype", 21)] =
                arrayOf("com.skype.raider", "com.skype.m2", "com.microsoft.office.lync15")
            map[Pair("Facebook", 22)] = arrayOf("com.facebook.katana", "com.facebook.lite")
            map[Pair("Twitter", 23)] = arrayOf("com.twitter.android", "com.twitter.android.lite")
            map[Pair("LinkedIn", 24)] = arrayOf("com.linkedin.android", "com.linkedin.android.lite")
            map[Pair("Instagram", 25)] = arrayOf("com.instagram.android")
            map[Pair("WhatsApp", 27)] = arrayOf("com.whatsapp", "com.whatsapp.w4b")
            map[Pair("Messenger", 28)] = arrayOf("com.facebook.orca", "com.facebook.mlite")
            return map
        }
    }
}