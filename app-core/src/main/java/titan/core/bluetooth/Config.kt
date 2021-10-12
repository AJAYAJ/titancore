package titan.core.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import java.util.*

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

/**
 * @author Raviteja Gadipudi
 */

/**
 * Holds device configuration, this config will be used to understand how to communicate with device.
 * This will be unique to every device compatible with application
 */

@androidx.annotation.Keep
data class DeviceConfig(
    val pinPairing: Boolean,
    val notify: Boolean,
    val mtu: Int,
    val dfu: Boolean,
    val services: List<DeviceService>,
    val dfuService: List<String>,
    val options: Options,
    val settings: Settings,
    val aboutBand:AboutBand
) {

    fun getRequiredServices(): HashSet<String> {
        /*Note: Do not add DFU Service in getRequiredServices(). In case if we need to modify this need to change the logic for this titan.core.products.TitanWearable.checkIsOnlyDFUServiceAvailbleORNot*/
        val requiredServices = HashSet<String>()
        for (service in services) {
            if (service.required) {
                requiredServices.add(service.uuid)
            }
        }
        return if (requiredServices.isEmpty()) hashSetOf() else requiredServices
    }

    fun isNotification(characteristic: BluetoothGattCharacteristic): Boolean {
        for (service in services) {
            if (UUID.fromString(service.uuid) == characteristic.service.uuid) {
                for (chara in service.characteristics) {
                    if (characteristic.uuid == UUID.fromString(chara.uuid)) {
                        return chara.isNotify()
                    }
                }
            }
        }
        return false
    }
}

@androidx.annotation.Keep
data class Options(
    val steps: Boolean,
    val sleep: Boolean,
    val heartRate: Boolean,
    val multiSport: Boolean
)

@androidx.annotation.Keep
data class Settings(
    val notifications: Boolean,
    val stepsGoal: Boolean,
    val sleepGoal: Boolean,
    val multiSportGoal: Boolean,
    val bandLayout: Boolean,
    val unitSystem: Boolean,
    val liftToView: Boolean,
    val sedentaryAlert: Boolean,
    val autoHeartRate: Boolean,
    val pair: Boolean,
    val autoSleep: Boolean,
    val findPhone: Boolean,
    val findBand: Boolean,
    val antiLost: Boolean,
    val alarm: Boolean,
    val factoryReset: Boolean,
    val dnd: Boolean,
    val about: Boolean,
    val ota: Boolean,
    val watchFace:Boolean,
    val eventLog: Boolean,
    val batteryOptimization: Boolean
)

@androidx.annotation.Keep
data class AboutBand(
    val termsAndConditionsURL:String,
    val privacyPolicyURL:String,
    val bandUserManualURL:String,
    val aboutTheProductURL:String,
    val aboutCompanyURL:String,

)

@androidx.annotation.Keep
data class DeviceService(
    val uuid: String,
    val characteristics: ArrayList<DeviceServiceCharacteristics>,
    val required: Boolean
)

@androidx.annotation.Keep
data class DeviceServiceCharacteristics(
    val uuid: String,
    val type: String,
    val cccd: String?
) {
    fun isNotify(): Boolean {
        return type.contains("N") || type.contains("I")
    }
}

/**
 * Holds product configuration at application level.
 * Helps to understand all supported products.
 */
@androidx.annotation.Keep
data class ProductConfig(
    val group: String,
    val products: List<Product>
)

/**
 * product information alone with filter required
 */
@androidx.annotation.Keep
data class Product(
    val name: String,
    val id: Int,
    val code: String,
    val filter: ProductFilter,
    var sortingID:Int
)

@androidx.annotation.Keep
data class ProductFilter(
    val UUIDs: List<String>,
    val name: String
)

private fun getUUID(prefix: String? = null, code: String, postFix: String? = null): UUID {
    return if (code.length == 4) {
        if (postFix == null || prefix == null) {
            UUID.fromString(code)
        } else {
            uuidFromShortCode16(prefix, code, postFix)
        }
    } else if (code.length == 8) {
        if (postFix == null) {
            UUID.fromString(code)
        } else {
            uuidFromShortCode32(code, postFix)
        }
    } else {
        UUID.fromString(code)
    }
}

private fun uuidFromShortCode16(prefix: String, code: String, postFix: String): UUID {
    return UUID.fromString("$prefix$code$postFix")
}

private fun uuidFromShortCode32(code: String, postFix: String): UUID {
    return UUID.fromString("$code$postFix")
}