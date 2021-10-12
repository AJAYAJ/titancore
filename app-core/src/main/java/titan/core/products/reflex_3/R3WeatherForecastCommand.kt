package titan.core.products.reflex_3

import titan.core.asInt
import titan.core.bluetooth.CommManager
import titan.core.products.*
import java.util.*

private const val commandId: Byte = 10
private const val keyId: Byte = 1

internal class R3WeatherForecastCommand : DataCommand {
    private var listener: DataCallback<R3WeatherForecast>? = null

    fun setData(weatherForecast: R3WeatherForecast): R3WeatherForecastCommand {
        val currentWeatherCond: WeatherConditions = WeatherConditions.SUNNY
        val currentTemperature: Int = 35
        val currentMaxTemp: Int = 40
        val currentMinTemp: Int = 18
        val humidity: Int = 40
        val uvIntensity: Int = 4
        val pollutionIndex: Int = 85
        val weatherCond1: WeatherConditions = WeatherConditions.CLOUDY
        val maxTemp1: Int = 42
        val minTemp1: Int = 35
        val weatherCond2: WeatherConditions = WeatherConditions.SANDSTORM
        val maxTemp2: Int = 44
        val minTemp2: Int = 15
        val weatherCond3: WeatherConditions = WeatherConditions.RAIN
        val maxTemp3: Int = 55
        val minTemp3: Int = 0
        val data: ByteArray = byteArrayOf(
            commandId,
            keyId,
            WeatherConditions.getValue(weatherForecast.currentWeatherCond),
            weatherForecast.currentTemp.toByte(),
            weatherForecast.currentMaxTemp.toByte(),
            weatherForecast.currentMinTemp.toByte(),
            humidity.toByte(),
            uvIntensity.toByte(),
            pollutionIndex.toByte(),
            WeatherConditions.getValue(weatherForecast.weatherCond1),
            weatherForecast.maxTemp1.toByte(),
            weatherForecast.minTemp1.toByte(),
            WeatherConditions.getValue(weatherForecast.weatherCond2),
            weatherForecast.maxTemp2.toByte(),
            weatherForecast.minTemp2.toByte(),
            WeatherConditions.getValue(weatherForecast.weatherCond3),
            weatherForecast.maxTemp3.toByte(),
            weatherForecast.minTemp3.toByte()
        )
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = data,
                key = getKey()
            )
        }
        return this
    }

    fun callback(listener: DataCallback<R3WeatherForecast>): R3WeatherForecastCommand {
        this.listener = listener
        return this
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != commandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            ResponseStatus.INCOMPATIBLE
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
        if (data.size < 3) {
            failed()
        } else {
            listener?.onResult(Response.Status(data[2].asInt() == 0))
        }
    }
}

data class R3WeatherForecast(
    val currentWeatherCond: WeatherConditions = WeatherConditions.OTHER,
    var currentTemp: Int,
    var currentMaxTemp: Int,
    var currentMinTemp: Int,
    val humidity: Int = 0,
    val uvIntensity: Int = 0,
    val pollutionIndex: Int = 0,
    val weatherCond1: WeatherConditions = WeatherConditions.OTHER,
    var maxTemp1: Int,
    var minTemp1: Int,
    val weatherCond2: WeatherConditions = WeatherConditions.OTHER,
    var maxTemp2: Int,
    var minTemp2: Int,
    val weatherCond3: WeatherConditions = WeatherConditions.OTHER,
    var maxTemp3: Int,
    var minTemp3: Int
)