package titan.core.products

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import titan.core.bluetooth.CommManager
import titan.core.bluetooth.ReflexProducts
import titan.core.products.reflex_2.*
import titan.core.products.reflex_3.*
import titan.core.products.reflex_slay.*
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

class Commands {
    private val ioDispatcher = CoroutineScope(Dispatchers.IO)
    private val mainDispatcher = CoroutineScope(Dispatchers.Main)
    private val commandsList = HashMap<UUID, DataCommand>()

    fun add(command: DataCommand) {
        command.getKey()?.let {
            commandsList[it] = command
        }
    }

    fun get(uuid: UUID): DataCommand? {
        return commandsList[uuid]
    }

    fun remove(uuid: UUID): DataCommand? {
        return commandsList.remove(uuid)
    }

    fun getDispatcher(main: Boolean = false): CoroutineScope {
        return if (main) {
            mainDispatcher
        } else {
            ioDispatcher
        }
    }

    fun setTime(context: Context, callback: DataCallback<Boolean>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2TimeCommand(context = context).set().callback(callback))
                }
                ReflexProducts.isReflex3(it) -> {
                    add(R3SetTimeCommand().set().callback(callback))
                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSSetTimeCommand().set().callback(callback))
                }
            }
        }
    }

    /*This method will return list of enabled menus in the watch*/
    @Suppress("unused")
    private fun getDisplayInterface(callback: DataCallback<CoreDisplayInterface>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2DisplayInterfaceCommand().get().callback(callback))
                }
            }
        }
    }

    fun setDisplayInterface(
        displayInterface: CoreDisplayInterface,
        callback: DataCallback<CoreDisplayInterface>
    ): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2DisplayInterfaceCommand().set(displayInterface).callback(callback))
                }
            }
        }
    }

    fun getDeviceInfo(callback: DataCallback<DeviceInfo>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2DeviceInfoCommand().get().callback(callback))
                }
                ReflexProducts.isReflex3(it) -> {
                    add(R3BasicInfoCommand().getData().callback(callback))
                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSDeviceStatusCommand().getData().callback(callback))
                }
            }
        }
    }

    fun getStepsAndSleep(date: Date, callback: DataCallback<CoreSteps>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2StepsSleepCommand().get(date).callback(callback))
                }
            }
        }
    }

    fun getSteps(date: Date, callback: DataCallback<CoreSteps>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2StepsSleepCommand().get(date).callback(callback))
                }
                ReflexProducts.isReflex3(it) -> {
                    add(R3StepsCommand().get().callback(callback))
                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSGetDailyRecordCommand().get().callback(callback))
                }
            }
        }
    }

    fun getStepsSleepAndHR(days: Int = 0, callback: DataCallback<CoreSteps>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSGetDailyRecordCommand().get(days).callback(callback))
                }
            }
        }
    }

    fun getStepsHistory(callback: DataCallback<CoreSteps>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3StepsHistoryCommand().getData().callback(callback))
                }
            }
        }
    }

    fun getHeartRateHistory(callback: DataCallback<CoreHeartRate>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3HeartRateHistoryCommand().get().callback(callback))
                }
            }
        }
    }

    fun getSleepHistory(callback: DataCallback<CoreSleep>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3SleepHistoryCommand().get().callback(callback))
                }
            }
        }
    }

    fun getSleep(date: Date, callback: DataCallback<CoreSleep>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3SleepCommand().get().callback(callback))
                }
            }
        }
    }

    fun getHistoricalStepsAndSleepDates(callback: DataCallback<CoreHistoricalDates>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(
                        R2StepsAndSleepHistoricalDatesCommand().get().callback(callback)
                    )
                }
            }
        }
    }

    fun getBaseTime(context: Context, callback: DataCallback<DeviceTime>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2BaseTimeCommand(context).get().callback(callback))
                }
                ReflexProducts.isReflex3(it) -> {
                    add(R3GetTimeCommand().get().callback(callback))
                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSGetTimeCommand().get().callback(callback))
                }
            }
        }
    }

    fun setBaseTime(context: Context, baseTime: DeviceTime, callback: DataCallback<DeviceTime>) {
        isBandReady {
            when (baseTime) {
                is DeviceTime.BaseTime -> {
                    add(R2BaseTimeCommand(context).set(baseTime).callback(callback))
                }
                is DeviceTime.R3DeviceTime -> {

                }
            }
        }
    }

    fun enableHeartRate(interval: Int, enable: Boolean, callback: DataCallback<Boolean>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(
                        R2AutoHeartRateCommand().set(interval, enable).callback(callback)
                    )
                }
                ReflexProducts.isReflex3(it) -> {
                    add(R3HeartRateMonitoringCommand().setData(interval, enable).callback(callback))
                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSAutoHRCommand().setData(enable).callback(callback))
                    add(RSHeartRateIntervalCommand().setData(interval).callback(callback))
                }
            }
        }
    }

    fun getHeartRate(date: Date, callback: DataCallback<CoreHeartRate>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2HeartRateCommand().get(date).callback(callback))
                }
                ReflexProducts.isReflex3(it) -> {
                    add(R3HeartRateCommand().get().callback(callback))
                }
            }
        }
    }

    fun getHistoricalHeartRateDates(callback: DataCallback<CoreHistoricalDates>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(
                        R2HistoricalHeartRateDatesCommand().get()
                            .callback(callback)
                    )
                }
            }
        }
    }

    private inline fun isBandReady(body: (String?) -> Unit): Boolean {
        if (CommManager.getInstance().isDeviceReady()) {
            body(CommManager.getInstance().wearable?.productInfo?.getCode())
            return true
        }
        return false
    }

    fun setUserInfo(userInfo: UserInfo, callback: DataCallback<UserInfo>): Boolean {
        return isBandReady {
            when (userInfo) {
                is UserInfo.R2UserInfo -> {
                    add(R2UserInfoCommand().set(userInfo).callback(callback))
                }
                is UserInfo.R3UserInfo -> {
                    add(R3UserInfoCommand().setData(userInfo).callback(callback))
                }
                is UserInfo.RSUserInfo -> {
                    add(RSUserInfoCommand().setData(userInfo).callback(callback))
                }
                is UserInfo.R3UserGoalSettings -> {
                    add(R3StepGoalSettings().setData(userInfo).callback(callback))
                }
                is UserInfo.R3SleepGoalSettings -> {
                    add(R3SleepGoalSettings().setData(userInfo).callback(callback))
                }
            }
        }
    }

    fun setUnits(userInfo: UserInfo, callback: DataCallback<UserInfo>): Boolean {
        return isBandReady {
            when (userInfo) {
                is UserInfo.R3UserInfo -> {
                    add(R3UnitCommand().setData(userInfo).callback(callback))
                }
            }
        }
    }

    fun getUserInfo(callback: DataCallback<UserInfo>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2UserInfoCommand().get().callback(callback))
                }
                ReflexProducts.isReflex3(it) -> {

                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSUserInfoCommand().get().callback(callback))
                }
            }
        }
    }

    fun setSedentaryReminder(
        sedentaryReminder: SedentaryReminder,
        callback: DataCallback<SedentaryReminder>
    ): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(
                        R2SedentaryReminderCommand().setData(sedentaryReminder).callback(
                            callback
                        )
                    )
                }
                ReflexProducts.isReflex3(it) -> {
                    add(
                        R3SedentaryReminderCommand().set(sedentaryReminder).callback(
                            callback
                        )
                    )
                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(
                        RSSetSedentaryTimeCommand().set(sedentaryReminder).callback(
                            callback
                        )
                    )
                }
            }
        }
    }

    fun getSedentaryReminder(callback: DataCallback<SedentaryReminder>) {
        isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2SedentaryReminderCommand().getData().callback(callback))
                }
                ReflexProducts.isReflex3(it) -> {

                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSGetSedentaryTimeCommand().get().callback(callback))
                }
            }
        }
    }

    fun setAutoSleep(
        autoSleep: AutoSleep,
        callback: DataCallback<AutoSleep>
    ): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(
                        R2AutoSleepCommand().set(autoSleep).callback(
                            callback
                        )
                    )
                }
                ReflexProducts.isReflex3(it) -> {
                    add(
                        R3SleepMonitorCommand().set(autoSleep).callback(
                            callback
                        )
                    )
                }
            }
        }
    }

    fun getAutoSleepTimings(callback: DataCallback<AutoSleep>) {
        isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2AutoSleepCommand().getData().callback(callback))
                }
                ReflexProducts.isReflex3(it) -> {

                }
            }
        }
    }

    fun setWristSelection(
        product: String,
        wristSelection: WristSelection,
        callback: DataCallback<WristSelection>
    ): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(
                        R2WristSelectionCommand().setData(product, wristSelection).callback(
                            callback
                        )
                    )
                }
            }
        }
    }

    @Suppress("unused")
    private fun setAutoHRSchedule(
        autoHRSchedule: AutoHRSchedule,
        callback: DataCallback<AutoHRSchedule>
    ) {
        isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2AutoHRTestScheduleCommand().setData(autoHRSchedule).callback(callback))
                }
            }
        }
    }

    fun setAntiLost(isEnable: Boolean, callback: DataCallback<Boolean>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2AntiLostCommand().setData(isEnable).callback(callback))
                }
            }
        }
    }

    @Suppress("unused")
    private fun setMultiMediaDisplayFunction(isOn: Boolean, callback: DataCallback<Boolean>) {
        isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(
                        R2MultiMediaDisplayOnOrOffCommand().setData(state = isOn).callback(callback)
                    )
                }
            }
        }
    }

    fun sendNotification(model: CoreNotificationModel, callback: DataCallback<Boolean>) {
        isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2NotificationCommand().sendNotification(model).callback(callback))
                }
                ReflexProducts.isReflex3(it) -> {
                    add(R3MessageReminderCommand().setData(model).callback(callback))
                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSSendNotificationCommand().sendNotification(model).callback(callback))
                }

            }
        }
    }

    fun sendCallNotification(model: CoreNotificationModel, callback: DataCallback<Boolean>) {
        isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2CallReminderCommand().setIncomingCallData(model).callback(callback))
                }
                ReflexProducts.isReflex3(it) -> {
                    add(R3CallNotificationCommand().setData(model).callback(callback))
                }
            }
        }
    }

    fun cancelNotification(model: CoreNotificationModel, callback: DataCallback<Boolean>) {
        isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3CancelNotificationCommand().setData(model).callback(callback))
                }
            }
        }
    }

    fun cameraNotification(callback: DataCallback<Boolean>) {
        isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2CameraCommand().cameraAcknowledgement().callback(callback))
                }
                ReflexProducts.isReflex3(it) -> {
                    add(R3EventCommand().setData(true).callback(callback))
                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSTakePictureCommand().cameraAcknowledgement().callback(callback))
                }
            }
        }
    }

    fun cameraEnterAndExitAcknowledgement(mode: CameraMode, callback: DataCallback<Boolean>) {
        isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2CameraCommand().cameraAcknowledgement().callback(callback))
                }
                ReflexProducts.isReflex3(it) -> {
                    add(R3CameraCommand().setData(mode).callback(callback))
                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSTakePictureCommand().cameraAcknowledgement().callback(callback))
                }
            }
        }
    }

    fun liftWristToView(data: CoreLiftWristToView, callback: DataCallback<Boolean>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(
                        R2LiftWristToViewCommand().setData(liftWristToView = data)
                            .callback(callback)
                    )
                }
                ReflexProducts.isReflex3(it) -> {
                    add(R3LiftWristToViewCommand().set(liftWristToView = data).callback(callback))
                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(
                        RSLiftWristToViewCommand().setData(liftWristToView = data)
                            .callback(callback)
                    )
                }
            }
        }
    }

    fun findMyBand(callback: DataCallback<Boolean>) {
        isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2FindBandCommand().findBand().callback(callback))
                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSFindWatchCommand().findBand().callback(callback))
                }
            }
        }
    }

    fun factoryReset(callback: DataCallback<Boolean>) {
        isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2FactoryResetCommand().factoryReset().callback(callback))
                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSFactoryResetCommand().factoryReset().callback(callback))
                }
            }
        }
    }

    fun getAlarm(callback: DataCallback<CoreAlarmSet>) {
        isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2AlarmCommand().getAlarmData().callback(callback))
                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSAlarmCommand().getAlarmData().callback(callback))
                }
            }
        }
    }

    fun setAlarm(list: CoreAlarmSet, callback: DataCallback<CoreAlarmSet>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2AlarmCommand().setAlarmData(list).callback(callback))
                }
                ReflexProducts.isReflex3(it) -> {
                    add(R3SetAlarmCommand().setData(list).callback(callback))
                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSAlarmCommand().setAlarmData(list).callback(callback))
                }
            }
        }
    }

    fun setDeviceInfo(deviceByteArray: ByteArray, callback: DataCallback<Boolean>) {
        isBandReady {
            when {
                ReflexProducts.isReflex2(it) -> {
                    add(R2SetDeviceInfoCommand().setDeviceInfo(deviceByteArray).callback(callback))
                }
            }
        }
    }

    fun getBandSupportedFunctions(callback: DataCallback<SupportedFeatures>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3SupportedFeaturesCommand().get().callback(callback))
                }
            }
        }
    }

    fun getExtendedBandSupportedFunctionsForR3(callback: DataCallback<SupportedFeatures>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3ExtendedFeaturesCommand().get().callback(callback))
                }
            }
        }
    }

    fun getAvailableDataCount(callback: DataCallback<R3ActiveDataCount>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3ActiveDataCountCommand().getData().callback(callback))
                }
            }
        }
    }

    fun getMultiSportDataCount(callback: DataCallback<Int>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3MultiSportActiveDataCountCommand().get().callback(callback))
                }
            }
        }
    }

    fun getMultiSportData(
        isV3Enabled: Boolean = false,
        callback: DataCallback<CoreMultiSport>
    ): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    if (isV3Enabled) {
                        add(R3MultiSportV3Command().get().callback(callback))
                    } else {
                        add(R3MultiSportCommand().get().callback(callback))
                    }
                }
            }
        }
    }

    fun getExerciseData(serialNumber: Int = 0, callback: DataCallback<CoreMultiSport>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSExerciseDataCommand().get(serialNumber).callback(callback))
                }
            }
        }
    }

    fun bindBand(callback: DataCallback<CoreBinding>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3BindCommand().bind().callback(callback))
                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSAskPairCommand().pair().callback(callback))
                }
            }
        }
    }

    fun setBindingCodeOnlyForR3(code: String, callback: DataCallback<BindingStatus>) {
        add(R3SetBindCode().setData(code).callback(callback))
    }

    fun startSync(callback: DataCallback<Boolean>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3StartSyncCommand().startManualSync().callback(callback))
                }
            }
        }
    }

    fun endSync(callback: DataCallback<Boolean>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3EndSyncCommand().endManualSync().callback(callback))
                }
            }
        }
    }

    fun setGeneralSettings(settings: RSSettings, callback: DataCallback<RSSettings>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSSettingsCommand().setData(settings).callback(callback))
                }
            }
        }
    }

    fun getGeneralSettings(callback: DataCallback<RSSettings>): Boolean {
       return isBandReady {
            when {
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSSettingsCommand().get().callback(callback))
                }
            }
        }
    }

    fun setStepTarget(target: RSStepTarget, callback: DataCallback<RSStepTarget>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSStepTargetCommand().setData(target).callback(callback))
                }
            }
        }
    }

    fun getStepTarget(callback: DataCallback<RSStepTarget>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSStepTargetCommand().get().callback(callback))
                }
            }
        }
    }

    fun getDNDSettings(callback: DataCallback<CoreDND>) {
        isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3GetDNDSettingsCommand().get().callback(callback))
                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSGetDNDCommand().get().callback(callback))
                }
            }
        }
    }

    fun setDNDSettings(dnd: CoreDND, callback: DataCallback<CoreDND>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3SetDNDCommand().set(dnd).callback(callback))
                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSSetDNDCommand().set(dnd).callback(callback))
                }
            }
        }
    }

    fun setFindPhone(findPhone: Boolean, callback: DataCallback<Boolean>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3FindPhoneCommand().setData(findPhone).callback(callback))
                }
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSFindPhoneCommand().findPhone(findPhone).callback(callback))
                }
            }
        }
    }


    fun setOTAMode(callback: DataCallback<R3OTAStatus>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3EnterOTAMode().setData().callback(callback))
                }
            }
        }
    }

    fun setWatchFace(bytes: ByteArray, callback: DataCallback<Boolean>) {
        isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    R3WatchFaces.setWatchFace(bytes, "watch2").callback(callback)
                }
            }
        }
    }

    fun deleteWatchFace(bytes: ByteArray, callback: DataCallback<Boolean>) {
        isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    R3WatchFaces.deleteWatchFace().callback(callback)
                }
            }
        }
    }

    fun setFastMode(callback: DataCallback<Boolean>) {
        isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    R3WatchFaces.setFastMode().callback(callback)
                }
            }
        }
    }

    fun enableFileCheckMode(callback: DataCallback<Boolean>) {
        isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    R3WatchFaces.setSlowMode().callback(callback)
                }
            }
        }
    }

    fun setWatchFile(watchInfo: WatchFaceInfo, callback: DataCallback<WatchFaceInfo>): Boolean {
        return isBandReady {
            when (watchInfo) {
                is WatchFaceInfo.R3CreateWatch -> {
                    add(R3WatchCreateFaceCommand().setWatchFace(watchInfo).callback(callback))
                }
                is WatchFaceInfo.R3WatchDeleteWatch -> {
                    add(R3WatchDeleteFaceCommand().setData(watchInfo).callback(callback))
                }
                is WatchFaceInfo.R3SetPRN -> {
                    add(R3WatchPRNCommand().setData(watchInfo).callback(callback))
                }
                is WatchFaceInfo.R3FastFileTransfer -> {
                    add(R3FileTransferModeCommand().setData(watchInfo).callback(callback))
                }
                is WatchFaceInfo.R3FileTransferCheckMode -> {
                    add(
                        R3CheckFastFileTransferStatusCommand().setData(watchInfo).callback(callback)
                    )
                }
                is WatchFaceInfo.R3TransferWatch -> {
                    add(R3WatchFaceTransmitCommand().setData(watchInfo).callback(callback))
                }
                is WatchFaceInfo.R3EndTransfer -> {
                    add(R3WatchEndTransferCommand().setData(watchInfo).callback(callback))
                }
            }
        }
    }

    fun setMusicSwitch(data: Boolean, callback: DataCallback<Boolean>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3MusicCommand().setData(data).callback(callback))
                }
            }
        }
    }

    /* fun unbindBand(callback: DataCallback<Boolean>) {
          isBandReady {
            when {
             ReflexProducts.isReflex3(it) -> {
                 add(R3UnBindCommand().unbind().callback(callback))
             }
         }
         }
     }*/

    companion object {

        private val instance = Commands()

        fun getInstance(): Commands {
            return instance
        }
    }

    fun setWeatherForecast(
        weatherForecast: R3WeatherForecast,
        callback: DataCallback<R3WeatherForecast>
    ) {
        isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3WeatherForecastCommand().setData(weatherForecast).callback(callback))
                }
            }
        }
    }

    fun setWeatherSwitch(data: Boolean, callback: DataCallback<Boolean>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflex3(it) -> {
                    add(R3WeatherForecastSwitchCommand().setData(data).callback(callback))
                }
            }
        }
    }

    fun sendWeather(model: CoreWeatherModel, callback: DataCallback<Boolean>) {
        isBandReady {
            when {
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSWeatherCommand().sendWeather(model).callback(callback))
                }

            }
        }
    }

    fun heartRateInterval(interval: Int, callback: DataCallback<Boolean>): Boolean {
        return isBandReady {
            when {
                ReflexProducts.isReflexSlay(it) -> {
                    add(RSHeartRateIntervalCommand().setData(interval).callback(callback))
                }
            }
        }
    }
}