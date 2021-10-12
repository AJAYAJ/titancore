package titan.core

import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.absoluteValue
import kotlin.math.pow

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

fun Byte.asHex(): String {
    return String.format("%02X", this.asInt())
}

fun Byte.asInt(): Int {
    return this.toUByte().toInt()
}

fun Byte.halfAsInt(firstHalf: Boolean = true): Int {
    return if (firstHalf) this.toInt() shr 4 else {
        this.convertToBinaryString().substring(4, 8).toDecimal().toInt()
    }
}

fun Byte.asLong(): Long {
    return this.toUByte().toLong()
}

fun ByteArray.toInt(bigEndian: Boolean = true): Int {
    when {
        isEmpty() -> {
            return 0
        }
        size == 1 -> {
            return this[0].asInt()
        }
        else -> {
            if (bigEndian) {
                return (((this[1].toInt() and 255) shl 8) or (this[0].toInt() and 255))
            } else {
                return (((this[0].toInt() and 255) shl 8) or (this[1].toInt() and 255))
            }

        }
    }
}

fun ByteArray.toLong(bigEndian: Boolean = true): Long {
    when {
        size <= 2 -> return this.toInt(bigEndian).toLong()
        size == 3 -> {
            return if (bigEndian) {
                (0 and 255 or (this[0].toInt() and 255 shl 8) or (this[1].toInt() and 255 shl 16) or (this[2].toInt() and 255 shl 24)).toLong()
            } else {
                (this[2].toInt() and 255 or (this[1].toInt() and 255 shl 8) or (this[0].toInt() and 255 shl 16) or (0 and 255 shl 24)).toLong()
            }
        }
        else -> {
            return if (bigEndian) {
                (this[0].toInt() and 255 or (this[1].toInt() and 255 shl 8) or (this[2].toInt() and 255 shl 16) or (this[3].toInt() and 255 shl 24)).toLong()
            } else {
                (this[3].toInt() and 255 or (this[2].toInt() and 255 shl 8) or (this[1].toInt() and 255 shl 16) or (this[0].toInt() and 255 shl 24)).toLong()
            }
        }
    }
}

fun Byte.convertToBinaryString(includePadding: Boolean = true): String {
    if (includePadding) {
        return Integer.toBinaryString(this.toUByte().toInt()).padStart(8, '0')
    } else {
        return Integer.toBinaryString(this.toUByte().toInt())
    }
}

fun Long.toDecimal(): Int {
    var number = this
    var decimalNumber = 0
    var i = 0
    var remainder: Long

    while (this.toInt() != 0) {
        remainder = this % 10
        number /= 10
        decimalNumber += (remainder * 2.0.pow(i.toDouble())).toInt()
        ++i
    }
    return decimalNumber
}

fun String.toDecimal(): Long {
    var sum: Long = 0
    this.reversed().forEachIndexed { k, v ->
        sum += v.toString().toInt() * pow(2, k)
    }
    return sum
}

fun pow(base: Int, exponent: Int) = base.toDouble().pow(exponent.toDouble()).toLong()

fun byteMerge(byte1: ByteArray, byte2: ByteArray): ByteArray {
    val byte3 = ByteArray(byte1.size + byte2.size)
    System.arraycopy(byte1, 0, byte3, 0, byte1.size)
    System.arraycopy(byte2, 0, byte3, byte1.size, byte2.size)
    return byte3
}

fun byteCopy(value: ByteArray, offset: Int, length: Int): ByteArray {
    if (offset > value.size) {
        return value
    }
    val maxLength = (value.size - offset).coerceAtMost(length)
    val copy = ByteArray(maxLength)
    System.arraycopy(value, offset, copy, 0, maxLength)
    return copy
}

fun Int.toBytes(): ByteArray {
    return byteArrayOf((this shr 8 and 255).toByte(), (this and 255).toByte())
}

fun Long.toBytes(): ByteArray {
    return byteArrayOf((this shr 8 and 255).toByte(), (this and 255).toByte())
}

fun Int.to32BitByte(): ByteArray {
    return this.toLong().to32BitByte()
}

fun Long.to32BitByte(): ByteArray {
    val bytes = ByteArray(4)
    bytes[3] = (this and 0xFFFF).toByte()
    bytes[2] = ((this ushr 8) and 0xFFFF).toByte()
    bytes[1] = ((this ushr 16) and 0xFFFF).toByte()
    bytes[0] = ((this ushr 24) and 0xFFFF).toByte()
    return bytes
}


fun Date.atStartOfDay(): Date {
    val calendar = getLocalCalendar()
    calendar.time = this
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time
}

fun Date.atEndOfDay(): Date {
    val calendar = getLocalCalendar()
    calendar.time = this
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    return calendar.time
}

fun Date.toMilliSeconds(): Long {
    val calendar = getLocalCalendar()
    calendar.time = this
    return calendar.timeInMillis
}

fun getLast7Days(): List<Date> {
    val list = ArrayList<Date>()
    val calender = getLocalCalendar()
    for (i in 1..7) {
        list.add(calender.time.atStartOfDay())
        calender.add(Calendar.DAY_OF_YEAR, -1)
    }
    return list
}

fun getDifferenceInDays(from: Date, to: Date): Int {
    val daysDiff: Long = to.time - from.time
    val dayCount = daysDiff / (24 * 60 * 60 * 1000)
    return dayCount.toInt()
}

fun differenceInMinutes(from: Long, to: Long): Int {
    return (((to - from) / 1000) / 60).toInt()
}

fun differenceInMinutesToCurrentTime(time: Long): Int {
    return (((Date().time - time) / 1000) / 60).toInt().absoluteValue
}

fun differenceInHoursToCurrentTime(time: Long): Int {
    val daysDiff: Long = Date().time - time
    val dayCount = daysDiff / (60 * 60 * 1000)
    return dayCount.toInt()
}
//
//fun Calendar.displayDateTime(is24HoursFormat: Boolean): String {
//    val cal = getLocalCalendar()
//    cal.add(Calendar.DATE, -1)
//    when {
//        DateUtils.isToday(this.time.time) -> {
//            return "${Core.getApplication().getString(R.string.today)} ${SimpleDateFormat(
//                if (is24HoursFormat) {
//                    "HH:mm"
//                } else {
//                    "hh:mm a"
//                }, Locale.getDefault()
//            ).format(time)}"
//        }
//        cal.time.atStartOfDay() == time.atStartOfDay() -> {
//            return "${Core.getApplication().getString(R.string.yesterday)} ${SimpleDateFormat(
//                if (is24HoursFormat) {
//                    "HH:mm"
//                } else {
//                    "hh:mm a"
//                }, Locale.getDefault()
//            ).format(time)}"
//        }
//        else -> {
//            return SimpleDateFormat(
//                if (is24HoursFormat) {
//                    "dd MMM yyyy HH:mm"
//                } else {
//                    "dd MMM yyyy hh:mm a"
//                }, Locale.getDefault()
//            ).format(time)
//        }
//    }
//}

fun Calendar.year(): Int {
    return this.get(Calendar.YEAR)
}

fun Calendar.month(): Int {
    return this.get(Calendar.MONTH) + 1
}

fun Calendar.day(): Int {
    return this.get(Calendar.DATE)
}

fun Calendar.hour(): Int {
    return this.get(Calendar.HOUR_OF_DAY)
}

fun Calendar.min(): Int {
    return this.get(Calendar.MINUTE)
}

fun Calendar.seconds(): Int {
    return this.get(Calendar.SECOND)
}

fun Date.toYear(): Int {
    val calendar: Calendar = getLocalCalendar()
    calendar.time = this
    return calendar.year()
}

fun Date.toMonth(): Int {
    val calendar: Calendar = getLocalCalendar()
    calendar.time = this
    return calendar.month()
}

fun Date.toDay(): Int {
    val calendar: Calendar = getLocalCalendar()
    calendar.time = this
    return calendar.day()
}

/* Calendar month starts with index zero*/
fun Triple<Int, Int, Int>.toDate(): Date {
    val calendar: Calendar = getLocalCalendar()
    calendar.set(this.first, this.second - 1, this.third)
    return calendar.time.atStartOfDay()
}

fun get7DaysFromStartAndEndDate(
    startDate: Date,
    endDate: Date,
    selectedDate: Date
): Pair<Date, Date> {
    val startCalendar: Calendar = getLocalCalendar()
    val endCalendar: Calendar = getLocalCalendar()
    val selectedCalendar: Calendar = getLocalCalendar()
    selectedCalendar.time = selectedDate.atStartOfDay()
    startCalendar.time = startDate.atStartOfDay()
    endCalendar.time = endDate.atStartOfDay()
    when {
        selectedDate.atStartOfDay().after(startCalendar.time) -> {
            if (getDifferenceInDays(startCalendar.time, selectedDate.atStartOfDay()) > 6) {
                selectedCalendar.add(Calendar.DAY_OF_MONTH, -6)
            } else {
                selectedCalendar.add(
                    Calendar.DAY_OF_MONTH,
                    -getDifferenceInDays(startCalendar.time, selectedDate.atStartOfDay())
                )
            }
            return Pair(selectedCalendar.time, selectedDate.atStartOfDay())
        }
        selectedDate.atStartOfDay().before(endCalendar.time) -> {
            if (getDifferenceInDays(selectedDate.atStartOfDay(), endCalendar.time) > 6) {
                selectedCalendar.add(Calendar.DAY_OF_MONTH, 6)
            } else {
                selectedCalendar.add(
                    Calendar.DAY_OF_MONTH,
                    getDifferenceInDays(selectedDate.atStartOfDay(), endCalendar.time.atStartOfDay())
                )
            }
            return Pair(selectedDate.atStartOfDay(), selectedCalendar.time.atStartOfDay())
        }
        else -> {
            return Pair(selectedDate.atStartOfDay(), selectedDate.atStartOfDay())
        }
    }
}

// TODO Remove this
fun getLocalCalendar(): Calendar {
    return Calendar.getInstance(Locale.ENGLISH)
}

fun missingPackets(packets: Set<Int>, size: Int): java.util.ArrayList<Byte> {
    val list = java.util.ArrayList<Byte>()
    if (packets.size != size) {
        for (i in 1..size) {
            if (!packets.contains(i)) {
                list.add(i.toByte())
            }
        }
    }
    return list
}

fun Date.YYYY_MM_DD_HH_MM_SS() : String{
    return try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        dateFormat.format(this)
    } catch (e: Exception) {
        e.printStackTrace()
        return this.toString()
    }
}

fun formatToStr(number: Int): String {
    return String.format("%02d", number)
}
//
//fun getNotificationType(packageID: String): CoreNotificationType {
//    return getSupportedFunctions()[packageID] ?: CoreNotificationType.NONE
//}
//
//fun getSupportedFunctions(): HashMap<String, CoreNotificationType> {
//    val supportedAppsForNotification: HashMap<String, CoreNotificationType> = hashMapOf()
//    supportedAppsForNotification["com.skype.raider"] = CoreNotificationType.SKYPE
//    supportedAppsForNotification["com.skype.m2"] = CoreNotificationType.SKYPE
//    supportedAppsForNotification["com.facebook.katana"] = CoreNotificationType.FACEBOOK
//    supportedAppsForNotification["com.facebook.lite"] = CoreNotificationType.FACEBOOK
//    supportedAppsForNotification["com.facebook.orca"] = CoreNotificationType.FACEBOOK_MESSENGER
//    supportedAppsForNotification["com.facebook.mlite"] = CoreNotificationType.FACEBOOK_MESSENGER
//    supportedAppsForNotification["com.facebook.talk"] = CoreNotificationType.FACEBOOK_MESSENGER
//    supportedAppsForNotification["com.twitter.android"] = CoreNotificationType.TWITTER
//    supportedAppsForNotification["com.twitter.android.lite"] = CoreNotificationType.TWITTER
//    supportedAppsForNotification["com.linkedin.android"] = CoreNotificationType.LINKED_IN
//    supportedAppsForNotification["com.linkedin.android.lite"] = CoreNotificationType.LINKED_IN
//    supportedAppsForNotification["com.instagram.android"] = CoreNotificationType.INSTAGRAM
//    supportedAppsForNotification["com.whatsapp"] = CoreNotificationType.WHATS_APP
//    supportedAppsForNotification["com.whatsapp.w4b"] = CoreNotificationType.WHATS_APP
//    supportedAppsForNotification["com.google.android.apps.messaging"] = CoreNotificationType.MESSAGE
//    return supportedAppsForNotification
//}