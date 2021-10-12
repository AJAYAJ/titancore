package titan.core.products.reflex_slay

import titan.core.bluetooth.CommManager
import titan.core.products.*
import titan.core.toDecimal
import java.util.*

private const val messageId: Byte = 50
private const val messageLength: Byte = 10
private const val endKey: Byte = 2
private const val endId: Byte = -1

internal class RSSettingsCommand : DataCommand {
    private var listener: DataCallback<RSSettings>? = null

    fun get(): RSSettingsCommand {
        val data = byteArrayOf(
            0,
            4
        )
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    RS_COMMUNICATION_SERVICE,
                    RS_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = data,
                key = getKey()
            )
        }
        return this
    }

    fun setData(settings: RSSettings): RSSettingsCommand {
        val setting: GeneralSettings = GeneralSettings.SET_ALL_ITEMS
        val settingsConfig = ""
        val notification = ""
        val settingsBuilder = StringBuilder()
        settingsBuilder.append(
            if (settings.settingsConfig.isMetric) settingsConfig.plus("0") else settingsConfig.plus(
                "1"
            )
        )
            .append(
                if (settings.settingsConfig.isEnglish) settingsConfig.plus("0") else settingsConfig.plus(
                    "1"
                )
            )
            .append(
                if (settings.settingsConfig.is24H) settingsConfig.plus("1") else settingsConfig.plus(
                    "0"
                )
            )
            .append(
                if (settings.settingsConfig.isRaiseToWake) settingsConfig.plus("0") else settingsConfig.plus(
                    "1"
                )
            )
            .append(
                if (settings.settingsConfig.isHr24H) settingsConfig.plus("0") else settingsConfig.plus(
                    "1"
                )
            )
            .append(
                if (settings.settingsConfig.isCelsius) settingsConfig.plus("0") else settingsConfig.plus(
                    "1"
                )
            )
        val notifyBuilder = StringBuilder()
        notifyBuilder.append(
            if (settings.notifications.isCallNotification) notification.plus("1") else notification.plus(
                "0"
            )
        )
            .append(
                if (settings.notifications.isMsgNotification) notification.plus("1") else notification.plus(
                    "0"
                )
            )
        val settingConfiguration = settingsBuilder.toString()
        val notificationSetting = notifyBuilder.toString()
        val data: ByteArray = byteArrayOf(
            messageLength,
            messageId,
            GeneralSettings.getMode(setting),
            settingConfiguration.reversed().toDecimal().toByte(),
            notificationSetting.reversed().toDecimal().toByte(),
            if (settings.isInterface1) 128.toByte() else 0.toByte(),
            if (settings.isInterface2) 128.toByte() else 0.toByte(),
            if (settings.isInterface3) 128.toByte() else 0.toByte(),
            if (settings.isInterface4) 128.toByte() else 0.toByte(),
            if (settings.isInterface5) 128.toByte() else 0.toByte(),
            if (settings.isInterface6) 128.toByte() else 0.toByte(),
            if (settings.isInterface7) 128.toByte() else 0.toByte()
        )
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    RS_COMMUNICATION_SERVICE,
                    RS_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = data,
                key = getKey()
            )
        }
        return this
    }

    fun callback(listener: DataCallback<RSSettings>): RSSettingsCommand {
        this.listener = listener
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != endId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 4) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(data = byteArray)
            ResponseStatus.COMPLETED
        }
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }

    fun parse(data: ByteArray) {
        listener?.onResult(
            Response.Status(true)
        )
    }

}

data class RSSettings(
    val setting: GeneralSettings = GeneralSettings.SET_ALL_ITEMS,
    val settingsConfig: SettingConfig,
    val notifications: Notifications,
    val isInterface1: Boolean,
    val isInterface2: Boolean,
    val isInterface3: Boolean,
    val isInterface4: Boolean,
    val isInterface5: Boolean,
    val isInterface6: Boolean,
    val isInterface7: Boolean
)

data class SettingConfig(
    val isMetric: Boolean,
    val isEnglish: Boolean,
    val is24H: Boolean,
    val isRaiseToWake: Boolean,
    val isHr24H: Boolean,
    val isCelsius: Boolean
)

data class Notifications(
    val isCallNotification: Boolean,
    val isMsgNotification: Boolean
)

enum class GeneralSettings {
    SET_ALL_ITEMS, SET_METRIC_IMPERIAL_SYSTEM, SET_ENGLISH_CHINESE, SET_12H_24H_DISPLAY_MODE, SET_RAISE_TO_WAKE, SET_24H_HR, SET_ANCS, SET_INTERFACE;

    companion object {
        fun getMode(x: GeneralSettings): Byte {
            return when (x) {
                SET_ALL_ITEMS -> 0.toByte()
                SET_METRIC_IMPERIAL_SYSTEM -> 1.toByte()
                SET_ENGLISH_CHINESE -> 2.toByte()
                SET_12H_24H_DISPLAY_MODE -> 3.toByte()
                SET_RAISE_TO_WAKE -> 4.toByte()
                SET_24H_HR -> 5.toByte()
                SET_ANCS -> 6.toByte()
                SET_INTERFACE -> 7.toByte()
            }
        }
    }
}