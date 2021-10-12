package titan.core.products

import java.io.Serializable
import java.util.*


/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */
sealed class UserInfo(
        private val isUserMale: Boolean,
        private val userHeight: Int
) {

    fun getStrideLength(): Int {
        return if (isUserMale) {
            (0.415 * userHeight).toInt()
        } else {
            (0.413 * userHeight).toInt()
        }
    }

    fun getRunLength(): Int {
        return (1.25 * getStrideLength()).toInt()
    }

    data class R2UserInfo(
            val age: Int,
            val isMale: Boolean,
            val height: Int,
            val weight: Int,
            val stepsTarget: Long,
            val sleepTarget: Int
    ) : UserInfo(isMale, height)

    data class R3UserInfo(
            val dateOfBirth: Date,
            val isMale: Boolean,
            val height: Int,
            val weight: Int,
            val stepsTarget: Long,
            val sleepTarget: Int,
            var isMetric: Boolean,
            val isCelsius: Boolean,
            val is24HourFormat: Boolean
    ) : UserInfo(isMale, height)

    data class RSUserInfo(
            val dateOfBirth: Date,
            val weight: Int,
            val height: Int,
            val isMale: Boolean,
            val maxHR: Int,
            val restingHR: Int
    ) : UserInfo(isMale, height)

    data class R3UserGoalSettings(
            val goalType: GoalType,
            val stepsTarget: Long,
            val sleepTarget: Int,
            val distanceTarget: Int = 0,
            val isUserMale: Boolean,
            val userHeight: Int
    ) : UserInfo(isUserMale, userHeight)

    data class R3SleepGoalSettings(
            val sleepTarget: Int,
            val isUserMale: Boolean,
            val userHeight: Int
    ) : UserInfo(isUserMale, userHeight)

}

sealed class DeviceTime {
    data class BaseTime(
            val calendar: Calendar = Calendar.getInstance(),
            val activeTimeZone: Int,
            val currentTimeZone: Int,
            val isMetric: Boolean
    ) : DeviceTime()

    data class R3DeviceTime(
            val calendar: Calendar = Calendar.getInstance(),
            val weekDay: DaysList
    ) : DeviceTime()

    data class RSDeviceTime(
        val calendar: Calendar = Calendar.getInstance()
    ) : DeviceTime()
}

data class SedentaryReminder(
        val list: ArrayList<Sedentary>,
        var restTime: Int,
        var isSedentaryOn: Boolean
)

data class Sedentary(
        val start: Pair<Int, Int>,
        val end: Pair<Int, Int>,
        val interval: Int,
        val isSedentaryOn: Boolean,
        val repeatSedentary: Boolean = true,
        val repeatDaysList: ArrayList<DaysList> = arrayListOf(
                DaysList.MONDAY,
                DaysList.TUESDAY,
                DaysList.WEDNESDAY,
                DaysList.THURSDAY,
                DaysList.FRIDAY,
                DaysList.SATURDAY,
                DaysList.SUNDAY
        )
)

data class CoreDND(
        var isOn: Boolean,
        val start: Pair<Int, Int> = Pair(0, 0),
        val end: Pair<Int, Int> = Pair(0, 0),
        val repeatWeekly: Boolean = false,
        val isTimeBased: Boolean = false,
        val repeatDaysList: ArrayList<DaysList> = arrayListOf(
                DaysList.MONDAY,
                DaysList.TUESDAY,
                DaysList.WEDNESDAY,
                DaysList.THURSDAY,
                DaysList.FRIDAY,
                DaysList.SATURDAY
        )
)

data class AutoSleep(
        var sleepStart: Pair<Int, Int>,
        var sleepEnd: Pair<Int, Int>,
        var napStart: Pair<Int, Int>,
        var napEnd: Pair<Int, Int>,
        var sleepReminderMinutes: Int,
        var napReminderMinutes: Int,
        var autoSleep: Boolean,
        var remindSleep: Boolean,
        var remindNap: Boolean
)

data class WristSelection(
        var isLeftHand: Boolean
)

data class CoreAutoHRDetection(
        var isAutoHRDetectEnabled: Boolean,
        var recurringPeriod: Int
)

data class AutoHRSchedule(
        var isHREnable: Boolean,
        var isHREnabledMorning: Boolean,
        var morningStart: Pair<Int, Int>,
        var morningEnd: Pair<Int, Int>,
        var isHREnabledAfterNoon: Boolean,
        var afterNoonStart: Pair<Int, Int>,
        var afterNoonEnd: Pair<Int, Int>,
        var isHREnabledNight: Boolean,
        var nightStart: Pair<Int, Int>,
        var nightEnd: Pair<Int, Int>
)

data class CoreHistoricalDates(
        var startDate: Date,
        var endDate: Date
)

data class CoreAlarmSet(
        val list: ArrayList<CoreAlarm>
)

data class CoreAlarm(
        var alarmId: Int,
        var hour: Int,
        var minute: Int,
        var description: String = "",
        var enableAlarm: Boolean,
        var repeatAlarmWeekly: Boolean,
        var alarmRepetitionDays: ArrayList<DaysList>,
        var isDelete: Boolean = false,
        var snoozeDuration: Int = 10,
        var alarmType: AlarmType = AlarmType.OTHER
)

enum class DaysList(value: Int) {
    SUNDAY(0), MONDAY(1), TUESDAY(2), WEDNESDAY(3), THURSDAY(4), FRIDAY(5), SATURDAY(6);

    companion object {
        private fun getDay(day: Int): DaysList {
            return when (day) {
                0 -> SUNDAY
                1 -> MONDAY
                2 -> TUESDAY
                3 -> WEDNESDAY
                4 -> THURSDAY
                5 -> FRIDAY
                6 -> SATURDAY
                else -> SUNDAY
            }
        }

        fun getDaysList(weekDay: DaysList): ArrayList<DaysList> {
            val list: ArrayList<DaysList> = arrayListOf()
            val day = weekDay.ordinal
            for (i in day..6) {
                list.add(getDay(i))
            }
            if (day - 1 >= 0) {
                for (i in 0 until day) {
                    list.add(getDay(i))
                }
            }
            return list
        }
    }
}

data class R2StepsSleepData(
        val date: Date,
        val steps: Long,
        val calories: Long,
        val distance: Long,
        val activeTime: Long,
        val sleepTime: Long,
        val restTime: Long,
        val walkingTime: Long,
        val slowWalkingTime: Long,
        val mediumWalkingTime: Long,
        val fastWalkingTime: Long,
        val slowRunningTime: Long,
        val mediumRunningTime: Long,
        val fastRunningTime: Long,
        val deviceBattery: Int,
        val stepsDistance: Long,
        val weight: Long,
        val stepsTarget: Long,
        val sleepTarget: Long,
        val totalSlots: Long,
        val totalPackets: Long,
        var sleepDetails: TreeMap<Int, CoreSleepMode>,
        var stepsDetails: TreeMap<Int, Long>
) : CoreSteps

sealed class CoreBinding {
    data class R3Binding(
            val bindingStatus: Boolean,
            val isBindingCodeRequired: Boolean,
            val bindingCodeLength: Int
    ) : CoreBinding()

    data class RSBinding(
        val pairingStatus: Boolean,
        val isBindingCodeRequired: Boolean,
        ) : CoreBinding()
}

enum class CoreSleepMode(val value: Int) {
    NONE(-1),
    AWAKE_SLEEP(0),
    VERY_SHALLOW_SLEEP(1),
    SHALLOW_SLEEP(2),
    REM_SLEEP(3),
    DEEP_SLEEP(4),
    ;

    companion object {
        fun reflex2Conversion(number: Int): CoreSleepMode {
            return when (number) {
                128 -> DEEP_SLEEP
                129 -> SHALLOW_SLEEP
                130 -> VERY_SHALLOW_SLEEP
                131 -> AWAKE_SLEEP
                else -> NONE
            }
        }

        fun reflex3Conversion(number: Int): CoreSleepMode {
            return when (number) {
                1 -> AWAKE_SLEEP
                2 -> SHALLOW_SLEEP
                3 -> DEEP_SLEEP
                else -> NONE
            }
        }

        fun reflexSlayConversion(number: Int): CoreSleepMode {
            return when (number) {
                250 -> DEEP_SLEEP
                251 -> VERY_SHALLOW_SLEEP
//                252 -> SHALLOW_SLEEP
                253 -> AWAKE_SLEEP
                else -> NONE
            }
        }
    }
}

interface DeviceInfo {}

interface SupportedFeatures {}

interface CoreSteps {}

interface CoreSleep {}

interface CoreMultiSport {}

interface CoreHeartRate {}

data class CoreNotificationModel(
        val data: ByteArray = byteArrayOf(),
        val index: Int = 0,
        val app: Int,
        val title: String = "",
        val message: String = ""
)

data class CoreLiftWristToView(
        var isEnabled: Boolean,
        var isSleepEnabled: Boolean,
        var displayDuration: Int = 3,
        var isTimeRange: Boolean = false,
        var startHour: Int = 0,
        var startMin: Int = 0,
        var endHour: Int = 23,
        var endMin: Int = 59,
)

data class CoreDisplayInterface(
        val userLogo: Boolean,
        val time: Boolean = true,
        val steps: Boolean = true,
        val calories: Boolean,
        val distanceCovered: Boolean,
        val activeTime: Boolean,
        val stepsGoalProgress: Boolean,
        val displayGoalReached: Boolean,
        val alarm: Boolean,
        val sedentary: Boolean,
        val notifications: Boolean,
        val callReminder: Boolean,
        val heartRate: Boolean,
        val reminder: Boolean
)

enum class CameraMode(val value: Int) {
    NONE(-1),
    BEGIN_TO_TAKE_PICTURE(1),
    START_CAMERA(241),
    EXIT_CAMERA(240),
    ;

    companion object {
        fun getMode(number: Int): CameraMode {
            return when (number) {
                1 -> BEGIN_TO_TAKE_PICTURE
                241 -> START_CAMERA
                240 -> EXIT_CAMERA
                else -> NONE
            }
        }

        fun getCameraModeForReflex3(number: Int): CameraMode {
            return when (number) {
                6 -> BEGIN_TO_TAKE_PICTURE
                10 -> START_CAMERA
                1 -> START_CAMERA
                11 -> EXIT_CAMERA
                else -> NONE
            }
        }

        fun getNewCameraModeForReflex3(number: Int): CameraMode {
            return when (number) {
                6 -> BEGIN_TO_TAKE_PICTURE
                7 -> START_CAMERA
                11 -> EXIT_CAMERA
                else -> NONE
            }
        }
    }
}

enum class HandleCallState(val value: Int) {
    NONE(-1),
    ANSWER_INCOMING_CALL(0),
    END_INCOMING_CALL(1);

    companion object {
        fun getMode(number: Int): HandleCallState {
            return when (number) {
                0 -> ANSWER_INCOMING_CALL
                1 -> END_INCOMING_CALL
                else -> NONE
            }
        }

        fun getCallStateForReflex3(number: Int): HandleCallState {
            return when (number) {
                12 -> ANSWER_INCOMING_CALL
                13 -> END_INCOMING_CALL
                else -> NONE
            }
        }

        fun getCallStateForReflex2(number: Int): HandleCallState {
            return when (number) {
                1 -> END_INCOMING_CALL
                else -> NONE
            }
        }

    }
}

enum class BandMode(value: Int) {
    NONE(-1),
    NORMAL_MODE(0),
    SLEEP_MODE(1),
    SPORTS_MODE(2)
}

enum class GoalType {
    STEP, CALORIE, DISTANCE;

    companion object {
        fun getValue(x: GoalType): Byte {
            return when (x) {
                STEP -> 0.toByte()
                CALORIE -> 1.toByte()
                DISTANCE -> 2.toByte()
            }
        }
    }
}

enum class BatteryStatusMode(value: Int) {
    NONE(-1),
    NORMAL(0),
    CHARGING(1),
    FULLY_CHARGED(2),
    LOW_BATTERY(3);

    companion object {
        fun getMode(number: Int): BatteryStatusMode {
            return when (number) {
                0 -> BatteryStatusMode.NORMAL
                1 -> BatteryStatusMode.CHARGING
                2 -> BatteryStatusMode.FULLY_CHARGED
                3 -> BatteryStatusMode.LOW_BATTERY
                else -> BatteryStatusMode.NONE
            }
        }
    }
}

enum class BindingStatus(value: Int) {
    NONE(-1),
    BOUNDED(1),
    UN_BOUNDED(2),
    RE_BOUND_REQUIRED(3)
}

enum class AlarmType(value: Int) {
    NONE(-1),
    GET_UP(0),
    SLEEP(1),
    TRAINING(2),
    TAKE_MEDICINE(3),
    DATE(4),
    PARTY(5),
    MEETING(6),
    OTHER(7);

    companion object {
        fun getReflex3Conversion(type: AlarmType): Byte {
            return when (type) {
                GET_UP -> 0.toByte()
                SLEEP -> 1.toByte()
                TRAINING -> 2.toByte()
                TAKE_MEDICINE -> 3.toByte()
                DATE -> 4.toByte()
                PARTY -> 5.toByte()
                MEETING -> 6.toByte()
                OTHER -> 7.toByte()
                else -> -1
            }
        }
    }
}

enum class NotificationType(value: Int) {
    NONE(-1),
    MSG_CONTACT_REMIND(0),
    MSG_NUMBER_REMIND(1),
    MSG_REMIND(2),
    SMS(3),
    EMAIL(4),
    QQ(5),
    WE_CHAT(6),
    WEIBO(7),
    FACE_BOOK(8),
    TWITTER(9),
    OTHERS(10),
    WHATS_APP(11),
    MESSENGER(12),
    INSTAGRAM(13),
    LINKED_IN(14),
    CALENDAR_EVENT(15),
    SKYPE(16),
    ALARM_EVENT(17),
    POKE_MAN(18),
    VKONTAKTE(19),
    LINE(20),
    VIBER(21),
    KAKAO_TALK(22),
    GMAIL(23),
    OUT_LOOK(24),
    SNAP_CHAT(25),
    TELEGRAM(26)
}

enum class SportsType(val value: Int) {
    NONE(-1),
    WALKING(0),
    RUNNING(1),
    RIDING(2),
    HIKING(3),
    SWIMMING(4),
    MOUNTAIN_CLIMBING(5),
    BADMINTON(6),
    OTHER_TRAINING(7),
    FITNESS(8),
    SPINNING(9),
    OVER_BALL(10),
    RUNNING_MACHINE(11),
    SIT_UPS(12),
    PUSH_UPS(13),
    DUMB_BELL(14),
    WEIGHT_LIFTING(15),
    AEROBICS(16),
    YOGA(17),
    ROPE_SKIPPING(18),
    TABLE_TENNIS(19),
    BASKET_BALL(20),
    FOOT_BALL(21),
    VOLLEY_BALL(22),
    TENNIS(23),
    GOLF(24),
    BASE_BALL(25),
    SKIING(26),
    ROLLER_SKATING(27),
    DANCING(28),
    GYM(29),
    ROLLER_MACHINE(30),
    PILATES(31),
    CROSS_TRAIN(32),
    CARDIO(33),
    ZUMBA(34),
    SQUARE_DANCE(35),
    PLANK(36),
    OUTDOOR_RUNNING(37),
    INDOOR_RUNNING(38),
    OUTDOOR_CYCLING(39),
    IN_DOOR_CYCLING(40),
    OUTDOOR_WALK(41),
    INDOOR_WALK(42),
    SWIMMING_IN_OPEN_WATER(43),
    ROWING_MACHINE(44),
    HIIT(45),
    CRICKET(46),
    ELLIPTICAL_MACHINE(47),
    CYCLING(999),
    MULTI_SPORT(1000);

    companion object {
        fun fromInt(number: Int): SportsType {
            return when (number) {
                -1 -> NONE
                0 -> WALKING
                1 -> RUNNING
                2 -> RIDING/*Bicycle*/
                3 -> HIKING
                4 -> SWIMMING
                5 -> MOUNTAIN_CLIMBING
                6 -> BADMINTON
                7 -> OTHER_TRAINING
                8 -> FITNESS
                9 -> SPINNING
                10 -> OVER_BALL
                11 -> RUNNING_MACHINE
                12 -> SIT_UPS
                13 -> PUSH_UPS
                14 -> DUMB_BELL
                15 -> WEIGHT_LIFTING
                16 -> AEROBICS
                17 -> YOGA
                18 -> ROPE_SKIPPING
                19 -> TABLE_TENNIS
                20 -> BASKET_BALL
                21 -> FOOT_BALL
                22 -> VOLLEY_BALL
                23 -> TENNIS
                24 -> GOLF
                25 -> BASE_BALL
                26 -> SKIING
                27 -> ROLLER_SKATING
                28 -> DANCING
                29 -> GYM
                30 -> ROLLER_MACHINE
                31 -> PILATES
                32 -> CROSS_TRAIN
                33 -> CARDIO
                34 -> ZUMBA
                35 -> SQUARE_DANCE
                36 -> PLANK
                37 -> OUTDOOR_RUNNING
                38 -> INDOOR_RUNNING
                39 -> OUTDOOR_CYCLING
                40 -> IN_DOOR_CYCLING
                41 -> OUTDOOR_WALK
                42 -> INDOOR_WALK
                43 -> SWIMMING_IN_OPEN_WATER
                44 -> ROWING_MACHINE
                45 -> HIIT
                46 -> CRICKET
                47 -> ELLIPTICAL_MACHINE
                999 -> CYCLING
                1000 -> MULTI_SPORT
                else -> NONE
            }
        }

        fun reflex3Conversion(number: Int): SportsType {
            return when (number) {
                0 -> NONE
                1 -> WALKING
                2 -> RUNNING
                3 -> RIDING/*Bicycle*/
                4 -> HIKING
                5 -> SWIMMING
                6 -> MOUNTAIN_CLIMBING
                7 -> BADMINTON
                8 -> OTHER_TRAINING
                9 -> FITNESS
                10 -> SPINNING
                11 -> ELLIPTICAL_MACHINE
                12 -> RUNNING_MACHINE
                13 -> SIT_UPS
                14 -> PUSH_UPS
                15 -> DUMB_BELL
                16 -> WEIGHT_LIFTING
                17 -> AEROBICS
                18 -> YOGA
                19 -> ROPE_SKIPPING
                20 -> TABLE_TENNIS
                21 -> BASKET_BALL
                22 -> FOOT_BALL
                23 -> VOLLEY_BALL
                24 -> TENNIS
                25 -> GOLF
                26 -> BASE_BALL
                27 -> SKIING
                28 -> ROLLER_SKATING
                29 -> DANCING
                31 -> ROLLER_MACHINE
                32 -> PILATES
                33 -> CROSS_TRAIN
                34 -> CARDIO
                35 -> ZUMBA
                36 -> SQUARE_DANCE
                37 -> PLANK
                38 -> GYM
                48 -> OUTDOOR_RUNNING
                49 -> INDOOR_RUNNING
                50 -> OUTDOOR_CYCLING
                51 -> IN_DOOR_CYCLING
                52 -> OUTDOOR_WALK
                53 -> INDOOR_WALK
                54 -> SWIMMING
                55 -> SWIMMING_IN_OPEN_WATER
                56 -> ELLIPTICAL_MACHINE
                57 -> ROWING_MACHINE
                58 -> HIIT
                75 -> CRICKET
                999 -> CYCLING
                1000 -> MULTI_SPORT
                else -> NONE
            }
        }

        fun reflexSlayConversion(number: Int): SportsType {
            return when (number) {
                0 -> NONE
                1 -> RUNNING
                2 -> CYCLING
                3 -> YOGA
                4 -> SPINNING
                5 -> ELLIPTICAL_MACHINE
                6 -> MULTI_SPORT
//                7 -> TABLE_TENNIS
//                8 -> BADMINTON
//                999 -> CYCLING
//                1000 -> MULTI_SPORT
                else -> NONE
            }
        }
    }
}

enum class LangSupport(value: Int) {
    ENGLISH(1),
    CHINESE(2),
    FRENCH(3),
    GERMAN(4),
    ITALIAN(5),
    SPANISH(6),
    JAPANESE(7),
    CZECH(8),
    ROMANIAN(9),
    LITHUANIAN(10),
    DUTCH(11),
    SLOVENIAN(12),
    HUNGARIAN(13),
    POLISH(14),
    RUSSIAN(15),
    UKRAINIAN(16);
}

enum class HRStatus(value: Int) {
    NONE(-1),
    HR_NOT_ENABLED(0),
    HR_MEASURING_WATCH_NOT_WORN(1),
    HR_MEASURING_WATCH_WORN(2),
    CHARGING(3);

    companion object {
        fun getMode(number: Int): HRStatus {
            return when (number) {
                0 -> HR_NOT_ENABLED
                1 -> HR_MEASURING_WATCH_NOT_WORN
                2 -> HR_MEASURING_WATCH_WORN
                3 -> CHARGING
                else -> NONE
            }
        }
    }
}

data class RSDailyRecord(
        val serialNumber: Int,
        val date: Date,
        val totalSteps: Long,
        val totalCalories: Long,
        val totalDistance: Long,
        val totalSleep: Long,
        var sleepDetails: TreeMap<Int, CoreSleepMode>,
        val stepsDetails: TreeMap<Int, Long>,
        val hrDetails: TreeMap<Int, Long>
) : CoreSteps

enum class ClockfaceId {
    INVALID, FACE1, FACE2, FACE3;

    companion object {
        fun getValue(x: ClockfaceId): Byte {
            return when (x) {
                INVALID -> 0.toByte()
                FACE1 -> 1.toByte()
                FACE2 -> 2.toByte()
                FACE3 -> 3.toByte()
            }
        }
    }

}

data class RSExerciseData(
    val serialNumber: Int,
    val sportType: SportsType,
    val avgHR: Int,
    val exerciseStartTime: Date,
    val totalExerciseTime: Long,
    val exerciseEndTime: Date,
    val totalSteps: Long,
    val avgSpeed: Long,
    val totalDistance: Long,
    val totalCalories: Long,
    val hrDetails: TreeMap<Int, Int> = TreeMap(),
    val stepsDetails: TreeMap<Int, Int> = TreeMap(),
    val calorieDetails: TreeMap<Int, Int> = TreeMap(),
    val distanceDetails: TreeMap<Int, Int> = TreeMap()
) : CoreMultiSport

enum class MusicState(value: Int) {
    NONE(-1),
    MUSIC_ENTER(0),
    MUSIC_EXIT(1),
    MUSIC_NEXT(2),
    MUSIC_PREV(3),
    MUSIC_VOL_UP(4),
    MUSIC_VOL_DOWN(5),
    MUSIC_PAUSE_OR_RESUME(7);

    companion object {
        fun getMusicModeForReflex3(number: Int): MusicState {
            return when (number) {
                1 -> MUSIC_ENTER
                2 -> MUSIC_PAUSE_OR_RESUME
                3 -> MUSIC_EXIT
                4 -> MUSIC_PREV
                5 -> MUSIC_NEXT
                8 -> MUSIC_VOL_UP
                9 -> MUSIC_VOL_DOWN
                else -> NONE
            }
        }
        fun getMusicModeForReflex2(number: Int): MusicState {
            return when (number) {
                0 -> MUSIC_EXIT
                1 -> MUSIC_ENTER
                2 -> MUSIC_PREV
                3 -> MUSIC_NEXT
                240 -> MUSIC_PAUSE_OR_RESUME
                else -> NONE
            }
        }
        fun getMusicModeForReflexSlay(number: Int): MusicState {
            return when (number) {
                1 -> MUSIC_PAUSE_OR_RESUME
                2 -> MUSIC_PAUSE_OR_RESUME
                3 -> MUSIC_PREV
                4 -> MUSIC_NEXT
                5 -> MUSIC_VOL_UP
                6 -> MUSIC_VOL_DOWN
                7 -> MUSIC_ENTER
                8 -> MUSIC_EXIT
                else -> NONE
            }
        }
    }
}

sealed class WatchFaceInfo() {
    data class R3WatchDeleteWatch(
            val fileName: ByteArray
    ) : WatchFaceInfo()

    data class R3FastFileTransfer(
            val isFastFileTransferOn: Boolean,
            val bandCurrentState: BandTransferState = BandTransferState.DEFAULT
    ) : WatchFaceInfo()

    data class R3CreateWatch(
            val fileName: String,
            val fileSize: Long
    ) : WatchFaceInfo()

    data class R3TransferWatch(
            val fileStream: ByteArray
    ) : WatchFaceInfo()

    data class R3SetPRN(
            val prn: Int = 10
    ) : WatchFaceInfo()

    data class R3EndTransfer(
            val canEndFileTransfer: Boolean = true,
            val checkSum:Int = 0
    ) : WatchFaceInfo()

    data class R3FileTransferCheckMode(
            val isCheckModeOn: Boolean,
            val bandCurrentState: BandTransferState = BandTransferState.DEFAULT
    ) : WatchFaceInfo()
}

enum class BandTransferState() {
    FAST,
    SLOW,
    DEFAULT
}

enum class WatchFaceTransferState() : Serializable {
    NONE,
    RECEIVED_SEND_NEXT_PACKET,
    OPERATION_TIMED_OUT,
    COMPLETED
}

enum class WeatherConditions {
    NONE, OTHER, SUNNY, CLOUDY, OVERCAST, RAIN, HEAVY_RAIN, THUNDERSTORM, SNOW, SLEET, TYPHOON, SANDSTORM, SUNNY_AT_NIGHT,
    CLOUDY_AT_NIGHT, HOT, COLD, COOL_BREEZE, STRONG_WIND, MIST, SHOWER, CLOUDY_TO_CLEAR;

    companion object {
        fun getValue(x: WeatherConditions): Byte {
            return when (x) {
                OTHER -> 0.toByte()
                SUNNY -> 1.toByte()
                CLOUDY -> 2.toByte()
                OVERCAST -> 3.toByte()
                RAIN -> 4.toByte()
                HEAVY_RAIN -> 5.toByte()
                THUNDERSTORM -> 6.toByte()
                SNOW -> 7.toByte()
                SLEET -> 8.toByte()
                TYPHOON -> 9.toByte()
                SANDSTORM -> 10.toByte()
                SUNNY_AT_NIGHT -> 11.toByte()
                CLOUDY_AT_NIGHT -> 12.toByte()
                HOT -> 13.toByte()
                COLD -> 14.toByte()
                COOL_BREEZE -> 15.toByte()
                STRONG_WIND -> 16.toByte()
                MIST -> 17.toByte()
                SHOWER -> 18.toByte()
                CLOUDY_TO_CLEAR -> 19.toByte()
                NONE -> (-1).toByte()
            }
        }
    }
}

enum class MusicMessage(value: Int) {
    NONE(-1),
    MUSIC_NAME(0),
    MUSIC_TOTAL_TIME(1),
    MUSIC_PLAY_TIME(2);

    companion object {
        fun getMusicMsg(number: Int): MusicMessage {
            return when (number) {
                1 -> MUSIC_NAME
                2 -> MUSIC_TOTAL_TIME
                3 -> MUSIC_PLAY_TIME
                else -> NONE
            }
        }
    }
}

data class CoreWeatherModel(
    val data: ByteArray = byteArrayOf(),
    val index: Int = 0
)

enum class RSWeatherConditions {
    NONE, CLEAR, CLOUD, WIND, RAIN;

    companion object {
        fun getValue(x: RSWeatherConditions): Byte {
            return when (x) {
                CLEAR -> 0.toByte()
                CLOUD -> 1.toByte()
                WIND -> 2.toByte()
                RAIN -> 3.toByte()
                NONE -> (-1).toByte()
            }
        }
    }
}