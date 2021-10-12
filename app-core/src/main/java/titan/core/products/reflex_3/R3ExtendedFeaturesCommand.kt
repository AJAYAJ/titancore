package titan.core.products.reflex_3

import titan.core.bluetooth.CommManager
import titan.core.convertToBinaryString
import titan.core.products.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Sai Vinay Mohan I.
 * Titan Company Ltd
 */

private const val commandId: Byte = 2
private const val keyId: Byte = 7

internal class R3ExtendedFeaturesCommand : DataCommand {
    private var listener: DataCallback<SupportedFeatures>? = null
    fun get(): R3ExtendedFeaturesCommand {
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
        } else if (byteArray.size < 4) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            parse(byteArray)
            ResponseStatus.COMPLETED
        }
    }

    fun callback(callback: DataCallback<SupportedFeatures>): R3ExtendedFeaturesCommand {
        listener = callback
        return this
    }

    fun parse(byteArray: ByteArray) {
        val binary = byteArray[3].convertToBinaryString().padStart(8, '0').reversed()
        val model = R3ExtendedSupportedFeatures(
            gpsFunction = binary[0] == '1',
            sleepTimePeriodSettings = binary[1] == '1',
            brightnessLevelAdjustment = binary[2] == '1',
            langSupport = getLanguageSupport(byteArray[2])
        )
        listener?.onResult(
            Response.Result(
                model
            )
        )
    }

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }

    private fun getLanguageSupport(byte: Byte): ArrayList<LangSupport> {
        val binary = byte.convertToBinaryString().padStart(8, '0').reversed()
        val availableLanguages: ArrayList<LangSupport> = arrayListOf()
        if (binary[0] == '1') {
            availableLanguages.add(LangSupport.ROMANIAN)
        }
        if (binary[1] == '1') {
            availableLanguages.add(LangSupport.LITHUANIAN)
        }
        if (binary[2] == '1') {
            availableLanguages.add(LangSupport.DUTCH)
        }
        if (binary[3] == '1') {
            availableLanguages.add(LangSupport.SLOVENIAN)
        }
        if (binary[4] == '1') {
            availableLanguages.add(LangSupport.HUNGARIAN)
        }
        if (binary[5] == '1') {
            availableLanguages.add(LangSupport.POLISH)
        }
        if (binary[6] == '1') {
            availableLanguages.add(LangSupport.RUSSIAN)
        }
        if (binary[7] == '1') {
            availableLanguages.add(LangSupport.UKRAINIAN)
        }
        return availableLanguages
    }

    private val uuid = UUID.randomUUID()

    override fun getKey(): UUID? {
        return uuid
    }
}

data class R3ExtendedSupportedFeatures(
    val gpsFunction: Boolean,
    val sleepTimePeriodSettings: Boolean,
    val brightnessLevelAdjustment: Boolean,
    val langSupport: ArrayList<LangSupport> = arrayListOf()
) : SupportedFeatures
