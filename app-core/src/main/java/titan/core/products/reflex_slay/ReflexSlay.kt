package titan.core.products.reflex_slay

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

const val RS_COMMUNICATION_SERVICE = "7658fd00-878a-4350-a93e-da553e719ed0"
const val RS_COMMUNICATION_WRITE_CHAR = "7658fd01-878a-4350-a93e-da553e719ed0"
const val RS_COMMUNICATION_NOTIFY_CHAR = "7658fd02-878a-4350-a93e-da553e719ed0"

class ReflexSlay internal constructor(private val context: Context) : TitanDevice {

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
            if (characteristic.uuid.toString().equals(RS_COMMUNICATION_NOTIFY_CHAR, true)) {
                if (RSTakePictureCommand().check(byteArray = value) == ResponseStatus.COMPLETED) {
                    coreLogger("Camera Command: ${CameraMode.getMode(value[2].asInt()).name}")
                    val intent = Intent(ACTION_CAMERA_STATE)
                    intent.putExtra(EXTRA_CAMERA_STATE, CameraMode.getMode(value[2].asInt()).name)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    return CommandResponseState.COMPLETED
                }
                if (RSFindPhoneCommand().check(byteArray = value) == ResponseStatus.COMPLETED) {
                    val intent = if (value.size > 2 && value[2].asInt() == 1) {
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
                if (RSMusicEventControlCommand().checkEventControl(byteArray = value) == ResponseStatus.COMPLETED) {
                    coreLogger("Music Command :${MusicState.getMusicModeForReflexSlay(value[2].asInt()).name}")
                    val intent = Intent(ACTION_MUSIC_STATE)
                    intent.putExtra(
                        EXTRA_MUSIC_STATE,
                        MusicState.getMusicModeForReflexSlay(value[2].asInt())
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
            is RSDeviceStatusCommand -> command.failed()
            is RSSetTimeCommand -> command.failed()
            is RSUserInfoCommand -> command.failed()
            is RSSettingsCommand -> command.failed()
            is RSStepTargetCommand -> command.failed()
            is RSGetDailyRecordCommand -> command.failed()
            is RSSetDNDCommand -> command.failed()
            is RSGetDNDCommand -> command.failed()
            is RSSetSedentaryTimeCommand -> command.failed()
            is RSGetSedentaryTimeCommand -> command.failed()
            is RSFindWatchCommand -> command.failed()
            is RSLiftWristToViewCommand -> command.failed()
            is RSAlarmCommand -> command.failed()
            is RSSendNotificationCommand -> command.failed()
            is RSAutoHRCommand -> command.failed()
            is RSFactoryResetCommand -> command.failed()
            is RSExerciseDataCommand -> command.failed()
            is RSFindPhoneCommand -> command.failed()
            is RSAskPairCommand -> command.failed()
            is RSWeatherCommand -> command.failed()
            is RSHeartRateIntervalCommand -> command.failed()
            is RSClearDailyRecordCommand -> command.failed()
            is RSClearExerciseDataCommand -> command.failed()
        }
    }

    private fun processCommand(command: DataCommand, value: ByteArray): ResponseStatus {
        return when (command) {
            is RSDeviceStatusCommand -> command.check(byteArray = value)
            is RSSetTimeCommand -> command.check(byteArray = value)
            is RSUserInfoCommand -> command.check(byteArray = value)
            is RSSettingsCommand -> command.check(byteArray = value)
            is RSStepTargetCommand -> command.check(byteArray = value)
            is RSGetDailyRecordCommand -> command.check(byteArray = value)
            is RSSetDNDCommand -> command.check(byteArray = value)
            is RSGetDNDCommand -> command.check(byteArray = value)
            is RSSetSedentaryTimeCommand -> command.check(byteArray = value)
            is RSGetSedentaryTimeCommand -> command.check(byteArray = value)
            is RSFindWatchCommand -> command.check(byteArray = value)
            is RSLiftWristToViewCommand -> command.check(byteArray = value)
            is RSAlarmCommand -> command.check(byteArray = value)
            is RSSendNotificationCommand -> command.check(byteArray = value)
            is RSAutoHRCommand -> command.check(byteArray = value)
            is RSFactoryResetCommand -> command.check(byteArray = value)
            is RSExerciseDataCommand -> command.check(byteArray = value)
            is RSFindPhoneCommand -> command.check(byteArray = value)
            is RSAskPairCommand -> command.check(byteArray = value)
            is RSWeatherCommand -> command.check(byteArray = value)
            is RSHeartRateIntervalCommand -> command.check(byteArray = value)
            is RSClearDailyRecordCommand -> command.check(byteArray = value)
            is RSClearExerciseDataCommand -> command.check(byteArray = value)
            else -> ResponseStatus.INCOMPATIBLE
        }
    }

    companion object {
        fun getProductInfo() = ProductInfo(6, ReflexProducts.REFLEX_SLAY.code)

        fun getDefaultNotifications(): Map<Pair<String, Int>, Array<String>> {
            val map = HashMap<Pair<String, Int>, Array<String>>()
            map[Pair("Call", 29)] = arrayOf("Call")
            return map
        }

        fun getSMSAppID(): Int {
            return 1
        }

        fun supportedAppsForNotifications(): Map<Pair<String, Int>, Array<String>> {
            val map = HashMap<Pair<String, Int>, Array<String>>()
            map[Pair("Redbus", 2)] = arrayOf("in.redbus.android")
            map[Pair("WhatsApp", 3)] = arrayOf("com.whatsapp", "com.whatsapp.w4b")
            map[Pair("Facebook", 4)] = arrayOf("com.facebook.katana", "com.facebook.lite")
            map[Pair("Twitter", 5)] = arrayOf("com.twitter.android", "com.twitter.android.lite")
            map[Pair("Messenger", 6)] = arrayOf("com.facebook.orca", "com.facebook.mlite")
            map[Pair("Instagram", 7)] = arrayOf("com.instagram.android")
            map[Pair("LinkedIn", 8)] = arrayOf("com.linkedin.android", "com.linkedin.android.lite")
            map[Pair("Calendar", 9)] = arrayOf("com.google.android.calendar")
            map[Pair("Youtube", 10)] = arrayOf(
                "com.google.android.apps.youtube.kids",
                "com.google.android.apps.youtube.music",
                "com.google.android.youtube"
            )
            map[Pair("Telegram", 11)] = arrayOf("org.telegram.messenger")
            map[Pair("Gmail", 12)] = arrayOf("com.google.android.gm", "com.google.android.gm.lite")
            map[Pair("Dailyhunt", 13)] = arrayOf("com.eterno", "com.eterno.go")
            map[Pair("Outlook", 14)] = arrayOf("com.microsoft.office.outlook")
            map[Pair("Snapchat", 15)] = arrayOf("com.snapchat.android")
            map[Pair("Hotstar", 16)] =
                arrayOf("in.startv.hotstar", "in.startv.hotstaronly", "in.startv.hotstar.dplus")
            map[Pair("Inshorts", 17)] = arrayOf("com.nis.app")
            map[Pair("Paytm", 18)] =
                arrayOf("net.one97.paytm", "com.paytm.business", "com.paytmmoney", "com.paytmmall")
            map[Pair("Amazon", 19)] = arrayOf("in.amazon.mShop.android.shopping")
            map[Pair("Flipkart", 20)] = arrayOf("com.flipkart.android")
            map[Pair("Prime", 21)] =
                arrayOf("com.amazon.avod.thirdpartyclient", "com.amazon.amazonvideo.livingroom")
            map[Pair("Netflix", 22)] = arrayOf("com.netflix.mediaclient")
            map[Pair("GPay", 23)] = arrayOf("com.google.android.apps.nbu.paisa.user")
            map[Pair("PhonePe", 24)] = arrayOf("com.phonepe.app")
            map[Pair("Swiggy", 25)] = arrayOf("in.swiggy.android")
            map[Pair("Zomato", 26)] = arrayOf("com.application.zomato")
            map[Pair("Make My Trip", 27)] = arrayOf("com.makemytrip")
            map[Pair("Jio Tv", 28)] = arrayOf("com.jio.jioplay.tv")
            map[Pair("Others", 30)] = arrayOf("")
//            map[Pair("Skype", 21)] =
//                arrayOf("com.skype.raider", "com.skype.m2", "com.microsoft.office.lync15")
            return map
        }
    }
}