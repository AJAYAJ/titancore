package titan.core.products.reflex_slay

import titan.core.bluetooth.CommManager
import titan.core.byteMerge
import titan.core.products.*
import java.util.*
import kotlin.collections.ArrayList

private const val endKey: Byte = 3
private const val endId: Byte = -1
private const val weatherId: Byte = 18
private const val numOfPackets: Byte = 8
private var weatherSerialNo: Int = -1

internal class RSWeatherCommand : DataCommand {
    private var listener: DataCallback<Boolean>? = null

    fun sendWeather(coreWeatherModel: CoreWeatherModel): RSWeatherCommand {
        weatherSerialNo = coreWeatherModel.index
        val data: ByteArray = byteMerge(
            byteArrayOf(
                numOfPackets,
                weatherId,
                coreWeatherModel.index.toByte(),
            ), coreWeatherModel.data
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

    fun callback(callback: DataCallback<Boolean>): RSWeatherCommand {
        listener = callback
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != endKey) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 2 && (byteArray[1] != endId)) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 3 && (byteArray[2] != weatherId)) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 4 && (byteArray[3] != 0.toByte())) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 5 && (byteArray[4] != weatherSerialNo.toByte())) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size < 5) {
            listener?.onResult(Response.Status(false))
            ResponseStatus.INVALID_DATA_LENGTH
        } else {
            listener?.onResult(Response.Status(true))
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
}

data class RSWeatherForecast(
    var cityName: String,
    val currentDay: RSWeatherDaily,
    val currentDayPlusOne: RSWeatherDaily,
    val currentDayPlusTwo: RSWeatherDaily,
    val currentDayPlusThree: RSWeatherDaily,
    val currentDayPlusFour: RSWeatherDaily,
    val currentDayPlusFive: RSWeatherDaily,
    val currentDayPlusSix: RSWeatherDaily,
    )

data class RSWeatherDaily(
    val weatherCond: WeatherConditions = WeatherConditions.OTHER,
    var currentTemp: Int,
    var maxTemp: Int,
    var minTemp: Int,
    val pollutionIndex: Int = 0
)