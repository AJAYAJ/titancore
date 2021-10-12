package titan.core.products.reflex_3

import com.google.gson.Gson
import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.convertToBinaryString
import titan.core.products.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

/*
main function -> 1Bytes
- fun_alarm_count Number of alarms(0- 20) -> 1 Byte
- fun_alarm_type alarm type -> 1 Byte
- fun_control control function -> 1 Byte
- fun_call_notify call alert -> 1Byte
- fun_msg_notify1 message reminder  -> 1Byte
- fun_other other functions -> 1Byte
- fun_msg_cfg message reminder configuration -> 1Byte
- fun_msg_notify2 message reminder 2 -> 1Byte
- fun_other2 other functions2 -> 1Byte
- fun_sport_type0 sport type0 -> 1Byte
- fun_sport_type1 sport type1 -> 1Byte
- fun_sport_type2 sport type2 -> 1Byte
- fun_sport_type3 sport type3 -> 1Byte
- Fun_main1 fastrack.reflex.main function1 -> 1Byte
- fun_msg_notify3 message reminder3 -> 1Byte
- fun_sport_num_show number of sport showed -> 1Byte
- fun_lang_type languages supported -> 1Byte
 */

private const val commandId: Byte = 2
private const val keyId: Byte = 2

internal class R3SupportedFeaturesCommand : DataCommand {
    private var listener: DataCallback<SupportedFeatures>? = null

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }

    fun get(): R3SupportedFeaturesCommand {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(commandId, keyId),
                key = getKey()
            )
        }
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != commandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 20) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun callback(callback: DataCallback<SupportedFeatures>): R3SupportedFeaturesCommand {
        listener = callback
        return this
    }

    fun parse(byteArray: ByteArray) {
        val mainFunction = getMainFunction(byteArray[2], byteArray[6])
        val model = R3SupportedFeatures(
            mainFunction = mainFunction,
            alarmCount = byteArray[3].asInt(),
            alarmType = getAlarmType(byteArray[4]),
            controlFunction = getControlFunction(byteArray[5]),
            callNotify = getCallNotification(byteArray[6]),
            msgNotify = getMessageNotifications(
                byteArray[7],
                byteArray[9],
                byteArray[10],
                byteArray[17]
            ),
            otherFunction = getOtherFunction(byteArray[8], byteArray[11]),
            sportType = getSportType(
                byteArray[12],
                byteArray[13],
                byteArray[14],
                byteArray[15]
            ),
            sportNumber = byteArray[18].toInt(),
            langSupport = getLanguageSupport(byteArray[19])
        )
        println(Gson().toJson(model).toString())
        listener?.onResult(
            Response.Result(
                model
            )
        )
    }

    private fun getLanguageSupport(byte: Byte): ArrayList<LangSupport> {
        val binary = byte.convertToBinaryString().padStart(8, '0').reversed()
        val availableLanguages: ArrayList<LangSupport> = arrayListOf()
        if (binary[0] == '1') {
            availableLanguages.add(LangSupport.CHINESE)
        }
        if (binary[1] == '1') {
            availableLanguages.add(LangSupport.ENGLISH)
        }
        if (binary[2] == '1') {
            availableLanguages.add(LangSupport.FRENCH)
        }
        if (binary[3] == '1') {
            availableLanguages.add(LangSupport.GERMAN)
        }
        if (binary[4] == '1') {
            availableLanguages.add(LangSupport.ITALIAN)
        }
        if (binary[5] == '1') {
            availableLanguages.add(LangSupport.SPANISH)
        }
        if (binary[6] == '1') {
            availableLanguages.add(LangSupport.JAPANESE)
        }
        if (binary[7] == '1') {
            availableLanguages.add(LangSupport.CZECH)
        }
        return availableLanguages
    }

    /*
    Total Response length is 1 Byte and byte is divided into 8 bits
    Bit 0 - Walking
    Bit 1 - Running
    Bit 2 - Riding
    Bit 3 - Hiking
    Bit 4 - Swimming
    Bit 5 - Mountain Climbing
    Bit 6 - Badminton
    Bit 7 - others

    Total Response length is 1 Byte and byte is divided into 8 bits
    Bit 0 - Fitness
    Bit 1 - Spinning Bike
    Bit 2 - Over Ball
    Bit 3 - Running Machine
    Bit 4 - Sit-up
    Bit 5 - Push-up
    Bit 6 - Dumbbell
    Bit 7 - Weight lifting

    Total Response length is 1 Byte and byte is divided into 8 bits
    Bit 0 - Aerobics
    Bit 1 - Yoga
    Bit 2 - Rope skipping
    Bit 3 - Table tennis
    Bit 4 - Basketball
    Bit 5 - Football
    Bit 6 - Volleyball
    Bit 7 - Tennis

    Total Response length is 1 Byte and byte is divided into 8 bits
    Bit 0 - Golf
    Bit 1 - Baseball
    Bit 2 - Skiing
    Bit 3 - Roller skating
    Bit 4 - Dancing
    Bit 5 - Gym
    Bit 6 - reserved
    Bit 7 - reserved

    Total Response length is 1 Byte and byte is divided into 8 bits
    Bit 0 - Chinese
    Bit 1 - English
    Bit 2 - French
    Bit 3 - German
    Bit 4 - Italian
    Bit 5 - Spanish
    Bit 6 - Japanese
    Bit 7 - Czech
    */
    private fun getSportType(
        byte: Byte,
        byte1: Byte,
        byte2: Byte,
        byte3: Byte
    ): ArrayList<SportsType> {
        val binary = byte.convertToBinaryString().padStart(8, '0').reversed()
        val binary1 = byte1.convertToBinaryString().padStart(8, '0').reversed()
        val binary2 = byte2.convertToBinaryString().padStart(8, '0').reversed()
        val binary3 = byte3.convertToBinaryString().padStart(8, '0').reversed()
        val availableSportsTypes: ArrayList<SportsType> = arrayListOf()
        if (binary[0] == '1') {
            availableSportsTypes.add(SportsType.WALKING)
        }
        if (binary[1] == '1') {
            availableSportsTypes.add(SportsType.RUNNING)
        }
        if (binary[2] == '1') {
            availableSportsTypes.add(SportsType.RIDING)
        }
        if (binary[3] == '1') {
            availableSportsTypes.add(SportsType.HIKING)
        }
        if (binary[4] == '1') {
            availableSportsTypes.add(SportsType.SWIMMING)
        }
        if (binary[5] == '1') {
            availableSportsTypes.add(SportsType.MOUNTAIN_CLIMBING)
        }
        if (binary[6] == '1') {
            availableSportsTypes.add(SportsType.BADMINTON)
        }
        if (binary[7] == '1') {
            availableSportsTypes.add(SportsType.OTHER_TRAINING)
        }
        if (binary1[0] == '1') {
            availableSportsTypes.add(SportsType.FITNESS)
        }
        if (binary1[1] == '1') {
            availableSportsTypes.add(SportsType.SPINNING)
        }
        if (binary1[2] == '1') {
            availableSportsTypes.add(SportsType.OVER_BALL)
        }
        if (binary1[3] == '1') {
            availableSportsTypes.add(SportsType.RUNNING_MACHINE)
        }
        if (binary1[4] == '1') {
            availableSportsTypes.add(SportsType.SIT_UPS)
        }
        if (binary1[5] == '1') {
            availableSportsTypes.add(SportsType.PUSH_UPS)
        }
        if (binary1[6] == '1') {
            availableSportsTypes.add(SportsType.DUMB_BELL)
        }
        if (binary1[7] == '1') {
            availableSportsTypes.add(SportsType.WEIGHT_LIFTING)
        }
        if (binary2[0] == '1') {
            availableSportsTypes.add(SportsType.AEROBICS)
        }
        if (binary2[1] == '1') {
            availableSportsTypes.add(SportsType.YOGA)
        }
        if (binary2[2] == '1') {
            availableSportsTypes.add(SportsType.ROPE_SKIPPING)
        }
        if (binary2[3] == '1') {
            availableSportsTypes.add(SportsType.TABLE_TENNIS)
        }
        if (binary2[4] == '1') {
            availableSportsTypes.add(SportsType.BASKET_BALL)
        }
        if (binary2[5] == '1') {
            availableSportsTypes.add(SportsType.FOOT_BALL)
        }
        if (binary2[6] == '1') {
            availableSportsTypes.add(SportsType.VOLLEY_BALL)
        }
        if (binary2[7] == '1') {
            availableSportsTypes.add(SportsType.TENNIS)
        }
        if (binary3[0] == '1') {
            availableSportsTypes.add(SportsType.GOLF)
        }
        if (binary3[1] == '1') {
            availableSportsTypes.add(SportsType.BASE_BALL)
        }
        if (binary3[2] == '1') {
            availableSportsTypes.add(SportsType.SKIING)
        }
        if (binary3[3] == '1') {
            availableSportsTypes.add(SportsType.ROLLER_SKATING)
        }
        if (binary3[4] == '1') {
            availableSportsTypes.add(SportsType.DANCING)
        }
        if (binary3[5] == '1') {
            availableSportsTypes.add(SportsType.GYM)
        }
        return availableSportsTypes
    }

    /*Total Response length is 1 Byte and byte is divided into 8 bits
    Bit 0 - Sedentary alert
    Bit 1 - Anti-lost reminder
    Bit 2 - Calling
    Bit 3 - Find phone
    Bit 4 - Find band
    Bit 5 - One-click setting restore
    Bit 6 - Wrist sense
    Bit 7 - Weather forecast*/


    /*Total Response length is 1 Byte and each byte is divided into 8 bits
    Bit 0 - Static heart rate(0: Dynamic heart rate, 1: Static heart rate)
    Bit 1 - Do not disturb mode
    Bit 2 - Display mode
    Bit 3 - Heart rate monitoring mode control
    Bit 4 - Two-way anti-lost
    Bit 5 - All smart reminders
    Bit 6 - Display flip 180°
    Bit 7 - Do not display heart rate zone values (0:display，1:no display)*/
    private fun getOtherFunction(byte: Byte, byte1: Byte): OtherFunction {
        val binary = byte.convertToBinaryString().padStart(8, '0').reversed()
        val binary1 = byte1.convertToBinaryString().padStart(8, '0').reversed()
        return OtherFunction(
            sedentaryAlert = binary[0] == '1',
            antiLostReminder = binary[1] == '1',
            calling = binary[2] == '1',
            findPhone = binary[3] == '1',
            findBand = binary[4] == '1',
            oneClickRestore = binary[5] == '1',
            wristSense = binary[6] == '1',
            weatherForecast = binary[7] == '1',
            staticHeartRate = binary1[0] == '1',
            notDisturbMode = binary1[1] == '1',
            displayMode = binary1[2] == '1',
            heartRateMode = binary1[3] == '1',
            twoWayAntiLost = binary1[4] == '1',
            allReminders = binary1[5] == '1',
            displayFlip = binary1[6] == '1',
            notDisplayHeartRateZone = binary1[7] == '1'
        )
    }

    /*Total Response length is 1 Byte and byte is divided into 8 bits
    Bit 0 - Message contact remind
    Bit 1 - Message number remind
    Bit 2 - Message remind
    Bit 3~Bit 7 - Reserved*/

    /*Total Response length is 1 Byte and byte is divided into 8 bits
    Bit 0 - SMS
    Bit 1 - email
    Bit 2 - QQ
    Bit 3 - Wechat
    Bit 4 - weibo
    Bit 5 - facebook
    Bit 6 - Twitter
    Bit 7 - others*/

    /*Total Response length is 1 Byte and byte is divided into 8 bits
    Bit 0 - WhatsApp
    Bit 1 - Messenger
    Bit 2 - Instagram
    Bit 3 - Linked in
    Bit 4 - calendar Event
    Bit 5 - Skype
    Bit 6 - Alarm event
    Bit 7 - pokemon*/

    /*Total Response length is 1 Byte and byte is devided into 8 bits
    Bit 0 - Vkontakte
    Bit 1 - Line
    Bit 2 - Viber
    Bit 3 - KakaoTalk
    Bit 4 - Gmail
    Bit 5 - Outlook
    Bit 6 - Snapchat
    Bit 7 - TELEGRAM*/
    private fun getMessageNotifications(
        byte: Byte,
        byte1: Byte,
        byte2: Byte,
        byte3: Byte
    ): ArrayList<NotificationType> {
        val binary = byte.convertToBinaryString().padStart(8, '0').reversed()
        val binary1 = byte1.convertToBinaryString().padStart(8, '0').reversed()
        val binary2 = byte2.convertToBinaryString().padStart(8, '0').reversed()
        val binary3 = byte3.convertToBinaryString().padStart(8, '0').reversed()
        val availableNotificationTypes: ArrayList<NotificationType> = arrayListOf()
        if (binary[0] == '1') {
            availableNotificationTypes.add(NotificationType.MSG_CONTACT_REMIND)
        }
        if (binary[1] == '1') {
            availableNotificationTypes.add(NotificationType.MSG_NUMBER_REMIND)
        }
        if (binary[2] == '1') {
            availableNotificationTypes.add(NotificationType.MSG_REMIND)
        }
        if (binary1[0] == '1') {
            availableNotificationTypes.add(NotificationType.SMS)
        }
        if (binary1[1] == '1') {
            availableNotificationTypes.add(NotificationType.EMAIL)
        }
        if (binary1[2] == '1') {
            availableNotificationTypes.add(NotificationType.QQ)
        }
        if (binary1[3] == '1') {
            availableNotificationTypes.add(NotificationType.WE_CHAT)
        }
        if (binary1[4] == '1') {
            availableNotificationTypes.add(NotificationType.WEIBO)
        }
        if (binary1[5] == '1') {
            availableNotificationTypes.add(NotificationType.FACE_BOOK)
        }
        if (binary1[6] == '1') {
            availableNotificationTypes.add(NotificationType.TWITTER)
        }
        if (binary1[7] == '1') {
            availableNotificationTypes.add(NotificationType.OTHERS)
        }
        if (binary2[0] == '1') {
            availableNotificationTypes.add(NotificationType.WHATS_APP)
        }
        if (binary2[1] == '1') {
            availableNotificationTypes.add(NotificationType.MESSENGER)
        }
        if (binary2[2] == '1') {
            availableNotificationTypes.add(NotificationType.INSTAGRAM)
        }
        if (binary2[3] == '1') {
            availableNotificationTypes.add(NotificationType.LINKED_IN)
        }
        if (binary2[4] == '1') {
            availableNotificationTypes.add(NotificationType.CALENDAR_EVENT)
        }
        if (binary2[5] == '1') {
            availableNotificationTypes.add(NotificationType.SKYPE)
        }
        if (binary2[6] == '1') {
            availableNotificationTypes.add(NotificationType.ALARM_EVENT)
        }
        if (binary2[7] == '1') {
            availableNotificationTypes.add(NotificationType.POKE_MAN)
        }
        if (binary3[0] == '1') {
            availableNotificationTypes.add(NotificationType.VKONTAKTE)
        }
        if (binary3[1] == '1') {
            availableNotificationTypes.add(NotificationType.LINE)
        }
        if (binary3[2] == '1') {
            availableNotificationTypes.add(NotificationType.VIBER)
        }
        if (binary3[3] == '1') {
            availableNotificationTypes.add(NotificationType.KAKAO_TALK)
        }
        if (binary3[4] == '1') {
            availableNotificationTypes.add(NotificationType.GMAIL)
        }
        if (binary3[5] == '1') {
            availableNotificationTypes.add(NotificationType.OUT_LOOK)
        }
        if (binary3[6] == '1') {
            availableNotificationTypes.add(NotificationType.SNAP_CHAT)
        }
        if (binary3[7] == '1') {
            availableNotificationTypes.add(NotificationType.TELEGRAM)
        }
        return availableNotificationTypes
    }

    /*
    Total Response length is 1 Byte and byte is divided into 8 bits
    Bit 0 - Call alert
    Bit 1 - call contact
    Bit 2 - call number
    Bit 3~Bit 7 - Reserved
    */
    private fun getCallNotification(byte: Byte): CallNotify {
        val binary = byte.convertToBinaryString().padStart(8, '0').reversed()
        return CallNotify(
            callAlert = binary[0] == '1',
            callContact = binary[1] == '1',
            callNumber = binary[2] == '1'
        )
    }

    /*Total Response length is 1 Byte and byte is divided into 8 bits
    Bit 0 - Photo shooting
    Bit 1 - Music
    Bit 2 - Hid shotting control
    Bit 3 - 5 hear rate intervals
    Bit 4 - Binding timeout confirmation
    Bit 5 - Fast sync
    Bit 6 - Extended functions
    Bit 7 - Reserved as 0*/
    private fun getControlFunction(byte: Byte): ControlFunction {
        val binary = byte.convertToBinaryString().padStart(8, '0').reversed()
        return ControlFunction(
            photoShooting = binary[0] == '1',
            music = binary[1] == '1',
            hideShootingControl = binary[2] == '1',
            heartRateIntervals = binary[3] == '1',
            bindingTimeoutConfirmation = binary[4] == '1',
            fastSync = binary[5] == '1',
            extendedFunctions = binary[6] == '1'
        )
    }

    /*Total Response length is 1 Byte and byte is divided into 8 bits
    Bit 0 - get-up
    Bit 1 - sleep
    Bit 2 - training
    Bit 3 - Take medicine
    Bit 4 - Date
    Bit 5 - Party
    Bit 6 - Meeting
    Bit 7 - Customize*/
    private fun getAlarmType(byte: Byte): ArrayList<AlarmType> {
        val binary = byte.convertToBinaryString().padStart(8, '0').reversed()
        val availableAlarmTypes: ArrayList<AlarmType> = arrayListOf()
        if (binary[0] == '1') {
            availableAlarmTypes.add(AlarmType.GET_UP)
        }
        if (binary[1] == '1') {
            availableAlarmTypes.add(AlarmType.SLEEP)
        }
        if (binary[2] == '1') {
            availableAlarmTypes.add(AlarmType.TRAINING)
        }
        if (binary[3] == '1') {
            availableAlarmTypes.add(AlarmType.TAKE_MEDICINE)
        }
        if (binary[4] == '1') {
            availableAlarmTypes.add(AlarmType.DATE)
        }
        if (binary[5] == '1') {
            availableAlarmTypes.add(AlarmType.PARTY)
        }
        if (binary[6] == '1') {
            availableAlarmTypes.add(AlarmType.MEETING)
        }
        if (binary[7] == '1') {
            availableAlarmTypes.add(AlarmType.OTHER)
        }
        return availableAlarmTypes
    }


    /*
    Total Response length is 1 Byte and each byte is divided into 8 bits
    Bit 0 - step counting
    Bit 1 - sleep monitoring
    Bit 2 - single motion x
    Bit 3 - real-time data
    Bit 4 - device upgrade
    Bit 5 - heart rate monitoring
    Bit 6 - message center
    Bit 7 - timeline"
    Total Response length is 1 Byte and byte is divided into 8 bits
    Bit 0 - Log in
    Bit 1 - hide services
    Bit 2 - dial 1 setting
    Bit 3 - shortcut key 1 setting
    Bit 4 - Separate unit setting
    Bit 5 - Blood pressure
    Bit 6 - We chat movement
    Bit 7 - Fine control of equipment(Discarded)*/
    private fun getMainFunction(byte: Byte, byte1: Byte): MainFunction {
        val binary = byte.convertToBinaryString().padStart(8, '0').reversed()
        val binary1 = byte1.convertToBinaryString().padStart(8, '0').reversed()
        return MainFunction(
            stepCounting = binary[0] == '1',
            sleepMonitoring = binary[1] == '1',
            singleMotion = binary[2] == '1',
            realTimeData = binary[3] == '1',
            deviceUpgrade = binary[4] == '1',
            heartRateMonitoring = binary[5] == '1',
            messageCenter = binary[6] == '1',
            timeline = binary[7] == '1',
            logIn = binary1[0] == '1',
            hideServices = binary1[1] == '1',
            dialSetting = binary1[2] == '1',
            shortcutSetting = binary1[3] == '1',
            unitSetting = binary1[4] == '1',
            bloodPressure = binary1[5] == '1',
            weChatMovement = binary1[6] == '1',
            fineControl = binary1[7] == '1'
        )
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }
}

data class MainFunction(
    val stepCounting: Boolean,
    val sleepMonitoring: Boolean,
    val singleMotion: Boolean,
    val realTimeData: Boolean,
    val deviceUpgrade: Boolean,
    val heartRateMonitoring: Boolean,
    val messageCenter: Boolean,
    val timeline: Boolean,
    val logIn: Boolean,
    val hideServices: Boolean,
    val dialSetting: Boolean,
    val shortcutSetting: Boolean,
    val unitSetting: Boolean,
    val bloodPressure: Boolean,
    val weChatMovement: Boolean,
    val fineControl: Boolean
)

data class ControlFunction(
    val photoShooting: Boolean,
    val music: Boolean,
    val hideShootingControl: Boolean,
    val heartRateIntervals: Boolean,
    val bindingTimeoutConfirmation: Boolean,
    val fastSync: Boolean,
    val extendedFunctions: Boolean
)

data class CallNotify(
    val callAlert: Boolean,
    val callContact: Boolean,
    val callNumber: Boolean
)

data class OtherFunction(
    val sedentaryAlert: Boolean,
    val antiLostReminder: Boolean,
    val calling: Boolean,
    val findPhone: Boolean,
    val findBand: Boolean,
    val oneClickRestore: Boolean,
    val wristSense: Boolean,
    val weatherForecast: Boolean,
    val staticHeartRate: Boolean,
    val notDisturbMode: Boolean,
    val displayMode: Boolean,
    val heartRateMode: Boolean,
    val twoWayAntiLost: Boolean,
    val allReminders: Boolean,
    val displayFlip: Boolean,
    val notDisplayHeartRateZone: Boolean
)

data class R3SupportedFeatures(
    val mainFunction: MainFunction,
    val alarmCount: Int,
    val alarmType: ArrayList<AlarmType> = arrayListOf(),
    val controlFunction: ControlFunction,
    val callNotify: CallNotify,
    val otherFunction: OtherFunction,
    val msgNotify: ArrayList<NotificationType> = arrayListOf(),
    val sportType: ArrayList<SportsType> = arrayListOf(),
    val sportNumber: Int,
    val langSupport: ArrayList<LangSupport> = arrayListOf()
) : SupportedFeatures

