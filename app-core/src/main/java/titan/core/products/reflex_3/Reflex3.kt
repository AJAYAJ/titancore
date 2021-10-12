package titan.core.products.reflex_3

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
import java.util.*

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

const val R3_COMMUNICATION_SERVICE = "00000af0-0000-1000-8000-00805f9b34fb"
const val R3_COMMUNICATION_WRITE_CHAR = "00000af6-0000-1000-8000-00805f9b34fb"
const val R3_COMMUNICATION_READ_CHAR = "00000af7-0000-1000-8000-00805f9b34fb"
const val R3_HEALTH_WRITE_CHAR = "00000af1-0000-1000-8000-00805f9b34fb"

class Reflex3 internal constructor(private val context: Context) : TitanDevice {

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
        if (response != null && response != ResponseStatus.INCOMPATIBLE && response != ResponseStatus.ITEM_MISSED && key != null) {
            Commands.getInstance().remove(key)
        } else {
            if (characteristic.uuid.toString().equals(R3_COMMUNICATION_READ_CHAR, true)) {
                if (R3FindPhoneCommand().checkEventControl(byteArray = value) == ResponseStatus.COMPLETED) {
                    val intent = if (value.size > 2 && value[2].asInt() == 0) {
                        Intent(FIND_PHONE_START)
                    } else {
                        Intent(FIND_PHONE_STOP)
                    }
                    if (value.size > 3) {
                        intent.putExtra(FIND_PHONE_TIMEOUT, value[3].asInt())
                    }
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    return CommandResponseState.INCOMPATIBLE
                }
                if (R3WatchFaceTransmitCommand().checkPNRCommand(byteArray = value) == ResponseStatus.COMPLETED) {
                    val intent = Intent(ACTION_WATCH_FACE_STATE)
                    intent.putExtra(
                        EXTRA_WATCH_FACE_STATE,
                        WatchFaceTransferState.RECEIVED_SEND_NEXT_PACKET
                    )
                    intent.putExtra(
                        EXTRA_WATCH_CHECK_CODE,
                        R3WatchFaceTransmitCommand().getCheckCode(byteArray = value)
                    )
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    return CommandResponseState.INCOMPATIBLE
                }
                if (R3WatchEndTransferCommand().checkEventCommand(byteArray = value) == ResponseStatus.COMPLETED) {
                    val intent = Intent(ACTION_WATCH_FACE_STATE)
                    intent.putExtra(
                        EXTRA_WATCH_FACE_STATE,
                        R3WatchEndTransferCommand().getEventCommand(byteArray = value)
                    )
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    return CommandResponseState.INCOMPATIBLE
                }
                if (R3CameraCommand().checkEventControl(byteArray = value) == ResponseStatus.COMPLETED) {
                    coreLogger("Camera Command: ${CameraMode.getNewCameraModeForReflex3(value[2].asInt()).name}")
                    val intent = Intent(ACTION_CAMERA_STATE)
                    intent.putExtra(
                        EXTRA_CAMERA_STATE,
                        CameraMode.getNewCameraModeForReflex3(value[2].asInt()).name
                    )
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    return CommandResponseState.INCOMPATIBLE
                }
                if (R3MusicEventControlCommand().checkEventControl(byteArray = value) == ResponseStatus.COMPLETED) {
                    coreLogger("Music Command :${MusicState.getMusicModeForReflex3(value[2].asInt()).name}")
                    val intent = Intent(ACTION_MUSIC_STATE)
                    intent.putExtra(
                        EXTRA_MUSIC_STATE,
                        MusicState.getMusicModeForReflex3(value[2].asInt())
                    )
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    return CommandResponseState.INCOMPATIBLE
                }
                if (R3CallNotificationCommand().checkEventControl(byteArray = value) == ResponseStatus.COMPLETED) {
                    coreLogger("Call End Command :${HandleCallState.getCallStateForReflex3(value[2].asInt())}")
                    val intent = Intent(ACTION_HANDLE_CALL_STATE)
                    intent.putExtra(
                        EXTRA_HANDLE_CALL_STATE,
                        HandleCallState.getCallStateForReflex3(value[2].asInt())
                    )
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    return CommandResponseState.INCOMPATIBLE
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
            is R3BasicInfoCommand -> command.failed()
            is R3SupportedFeaturesCommand -> command.failed()
            is R3ExtendedFeaturesCommand -> command.failed()
            is R3SetTimeCommand -> command.failed()
            is R3StepsCommand -> command.failed()
            is R3ActiveDataCountCommand -> command.failed()
            is R3StepsHistoryCommand -> command.failed()
            is R3UserInfoCommand -> command.failed()
            is R3UnitCommand -> command.failed()
            is R3HeartRateMonitoringCommand -> command.failed()
            is R3HeartRateCommand -> command.failed()
            is R3HeartRateHistoryCommand -> command.failed()
            is R3SleepCommand -> command.failed()
            is R3SleepHistoryCommand -> command.failed()
            is R3BindCommand -> command.failed()
            is R3SetBindCode -> command.failed()
            is R3StartSyncCommand -> command.failed()
            is R3EndSyncCommand -> command.failed()
            is R3StepGoalSettings -> command.failed()
            is R3LiftWristToViewCommand -> command.failed()
            is R3SedentaryReminderCommand -> command.failed()
            is R3SleepGoalSettings -> command.failed()
            is R3SleepMonitorCommand -> command.failed()
            is R3SetDNDCommand -> command.failed()
            is R3GetDNDSettingsCommand -> command.failed()
            is R3SetAlarmCommand -> command.failed()
            is R3FindPhoneCommand -> command.failed()
            is R3EnterOTAMode -> command.failed()
            is R3GetTimeCommand -> command.failed()
            is R3CameraCommand -> command.failed()
            is R3MultiSportV3Command -> command.failed()
            is R3MessageReminderCommand -> command.failed()
            is R3CallNotificationCommand -> command.failed()
            is R3MusicCommand -> command.failed()
            is R3FileTransferModeCommand -> command.failed()
            is R3WatchCreateFaceCommand -> command.failed()
            is R3WatchDeleteFaceCommand -> command.failed()
            is R3WatchEndTransferCommand -> command.failed()
            is R3WatchFaceTransmitCommand -> command.failed()
            is R3WatchPRNCommand -> command.failed()
            is R3WeatherForecastCommand -> command.failed()
            is R3WeatherForecastSwitchCommand -> command.failed()
            is R3CheckFastFileTransferStatusCommand -> command.failed()
        }
    }

    private fun processCommand(command: DataCommand, value: ByteArray): ResponseStatus {
        return when (command) {
            is R3BasicInfoCommand -> command.check(byteArray = value)
            is R3SupportedFeaturesCommand -> command.check(byteArray = value)
            is R3ExtendedFeaturesCommand -> command.check(byteArray = value)
            is R3SetTimeCommand -> command.check(byteArray = value)
            is R3StepsCommand -> command.check(byteArray = value)
            is R3ActiveDataCountCommand -> command.check(byteArray = value)
            is R3StepsHistoryCommand -> command.check(byteArray = value)
            is R3UserInfoCommand -> command.check(byteArray = value)
            is R3UnitCommand -> command.check(byteArray = value)
            is R3HeartRateMonitoringCommand -> command.check(byteArray = value)
            is R3HeartRateCommand -> command.check(byteArray = value)
            is R3HeartRateHistoryCommand -> command.check(byteArray = value)
            is R3SleepCommand -> command.check(byteArray = value)
            is R3SleepHistoryCommand -> command.check(byteArray = value)
            is R3MultiSportActiveDataCountCommand -> command.check(byteArray = value)
            is R3MultiSportCommand -> command.check(byteArray = value)
            is R3HeartRateIntervalSettingCommand -> command.check(byteArray = value)
            is R3BindCommand -> command.check(byteArray = value)
            is R3SetBindCode -> command.check(byteArray = value)
            is R3StartSyncCommand -> command.check(byteArray = value)
            is R3EndSyncCommand -> command.check(byteArray = value)
            is R3StepGoalSettings -> command.check(byteArray = value)
            is R3LiftWristToViewCommand -> command.check(byteArray = value)
            is R3SedentaryReminderCommand -> command.check(byteArray = value)
            is R3SleepGoalSettings -> command.check(byteArray = value)
            is R3SleepMonitorCommand -> command.check(byteArray = value)
            is R3SetDNDCommand -> command.check(byteArray = value)
            is R3GetDNDSettingsCommand -> command.check(byteArray = value)
            is R3SetAlarmCommand -> command.check(byteArray = value)
            is R3FindPhoneCommand -> command.check(byteArray = value)
            is R3EnterOTAMode -> command.check(byteArray = value)
            is R3GetTimeCommand -> command.check(byteArray = value)
            is R3CameraCommand -> command.check(byteArray = value)
            is R3MultiSportV3Command -> command.check(byteArray = value)
            is R3MessageReminderCommand -> command.check(byteArray = value)
            is R3CallNotificationCommand -> command.check(byteArray = value)
            is R3MusicCommand -> command.check(byteArray = value)
            is R3FileTransferModeCommand -> command.check(byteArray = value)
            is R3WatchCreateFaceCommand -> command.check(byteArray = value)
            is R3WatchDeleteFaceCommand -> command.check(byteArray = value)
            is R3WatchEndTransferCommand -> command.check(byteArray = value)
            is R3WatchFaceTransmitCommand -> command.check(byteArray = value)
            is R3WatchPRNCommand -> command.check(byteArray = value)
            is R3WeatherForecastCommand -> command.check(byteArray = value)
            is R3WeatherForecastSwitchCommand -> command.check(byteArray = value)
            is R3CheckFastFileTransferStatusCommand -> command.check(byteArray = value)
            else -> ResponseStatus.INCOMPATIBLE
        }
    }

    companion object {
        internal fun getProductInfo() = ProductInfo(4, ReflexProducts.REFLEX_3.code)

        fun getDefaultNotifications(): Map<Pair<String, Int>, Array<String>> {
            val map = HashMap<Pair<String, Int>, Array<String>>()
            map[Pair("Call", 1)] = arrayOf("Call")
            return map
        }

        fun getSMSAppID(): Int {
            return 1
        }

        fun supportedAppsForNotifications(): Map<Pair<String, Int>, Array<String>> {
            val map = HashMap<Pair<String, Int>, Array<String>>()
            map[Pair("Redbus", 39)] = arrayOf("in.redbus.android")
            map[Pair("WhatsApp", 8)] = arrayOf("com.whatsapp", "com.whatsapp.w4b")
            map[Pair("Skype", 13)] =
                arrayOf("com.skype.raider", "com.skype.m2", "com.microsoft.office.lync15")
            map[Pair("Facebook", 6)] = arrayOf("com.facebook.katana", "com.facebook.lite")
            map[Pair("Twitter", 7)] = arrayOf("com.twitter.android", "com.twitter.android.lite")
            map[Pair("Messenger", 9)] = arrayOf("com.facebook.orca", "com.facebook.mlite")
            map[Pair("Instagram", 10)] = arrayOf("com.instagram.android")
            map[Pair("LinkedIn", 11)] = arrayOf("com.linkedin.android", "com.linkedin.android.lite")
            map[Pair("Calendar", 12)] = arrayOf("com.google.android.calendar")
            map[Pair("Youtube", 36)] = arrayOf(
                "com.google.android.apps.youtube.kids",
                "com.google.android.apps.youtube.music",
                "com.google.android.youtube"
            )
            map[Pair("Telegram", 23)] = arrayOf("org.telegram.messenger")
            map[Pair("Gmail", 20)] = arrayOf("com.google.android.gm", "com.google.android.gm.lite")
            map[Pair("Dailyhunt", 40)] = arrayOf("com.eterno", "com.eterno.go")
            map[Pair("Outlook", 21)] = arrayOf("com.microsoft.office.outlook")
            map[Pair("Snapchat", 22)] = arrayOf("com.snapchat.android")
            map[Pair("Hotstar", 41)] =
                arrayOf("in.startv.hotstar", "in.startv.hotstaronly", "in.startv.hotstar.dplus")
            map[Pair("Inshorts", 42)] = arrayOf("com.nis.app")
            map[Pair("Paytm", 43)] =
                arrayOf("net.one97.paytm", "com.paytm.business", "com.paytmmoney", "com.paytmmall")
            map[Pair("Amazon", 44)] = arrayOf("in.amazon.mShop.android.shopping")
            map[Pair("Flipkart", 45)] = arrayOf("com.flipkart.android")
            map[Pair("Prime", 46)] =
                arrayOf("com.amazon.avod.thirdpartyclient", "com.amazon.amazonvideo.livingroom")
            map[Pair("Netflix", 47)] = arrayOf("com.netflix.mediaclient")
            map[Pair("GPay", 48)] = arrayOf("com.google.android.apps.nbu.paisa.user")
            map[Pair("PhonePe", 49)] = arrayOf("com.phonepe.app")
            map[Pair("Swiggy", 50)] = arrayOf("in.swiggy.android")
            map[Pair("Zomato", 51)] = arrayOf("com.application.zomato")
            map[Pair("Make My Trip", 52)] = arrayOf("com.makemytrip")
            map[Pair("Jio Tv", 53)] = arrayOf("com.jio.jioplay.tv")
            map[Pair("Others", 14)] = arrayOf("")
            return map
        }
    }
}