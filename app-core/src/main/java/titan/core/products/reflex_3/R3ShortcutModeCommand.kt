package titan.core.products.reflex_3

import android.os.Message
import titan.core.bluetooth.CommManager
import titan.core.products.BaseParser
import titan.core.products.ResponsePackage
import titan.core.products.ResponseStatus
import titan.core.products.TaskPackage
import titan.core.toDecimal


/*    03-2E-FF-FF-FF-1F
    Position0: Command_Id
    Position1: Key_Id
    Position2: sport_ shortcut 0
    walking,running,cycling,hiking,swimming,climbing, badminton,other
    Position3: sport_ shortcut 1
    Fitness,Spinning,elliptical,Treadmill,Sit-ups,Push-ups,dumbbells,weights
    Position4: sport_ shortcut 2
    Aerobics,Yoga,Skipping,Table tennis,Basketball,Football,Volleyball,tennis
    Position5: sport_ shortcut 3
    Golf,Baseball,Shi,Skating,dance,reserved,reserved,reserved
    Remaining 14 bytes are reserved*/

private const val commandId: Byte = 3
private const val keyId: Byte = 46
const val R3_SHORTCUT_MODE_SETTING = "r3_shortcut_mode_setting"

internal object R3ShortcutModeCommand {
    fun setData() {
        val sportShortcut: ArrayList<SportShortcut0> = ArrayList()
        val sportShortcut1: ArrayList<SportShortcut1> = ArrayList()
        val sportShortcut2: ArrayList<SportShortcut2> = ArrayList()
        val sportShortcut3: ArrayList<SportShortcut3> = ArrayList()
        val data: ByteArray = byteArrayOf(
            commandId,
            keyId,
            sportShortcut(sportShortcut),
            sportShortcut1(sportShortcut1),
            sportShortcut2(sportShortcut2),
            sportShortcut3(sportShortcut3)
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
                key = null
            )
        }
    }

    private fun sportShortcut(sportShortcut: ArrayList<SportShortcut0>): Byte {
        var sportType0 = ""
        enumValues<SportShortcut0>().forEach {
            sportType0 = if (sportShortcut.contains(it)) {
                sportType0.plus("1")
            } else {
                sportType0.plus("0")
            }
        }
        return sportType0.reversed().toDecimal().toByte()
    }

    private fun sportShortcut1(sportShortcut1: ArrayList<SportShortcut1>): Byte {
        var sportType1 = ""

        enumValues<SportShortcut1>().forEach {
            sportType1 = if (sportShortcut1.contains(it)) {
                sportType1.plus("1")
            } else {
                sportType1.plus("0")
            }
        }
        return sportType1.reversed().toDecimal().toByte()
    }

    private fun sportShortcut2(sportShortcut2: ArrayList<SportShortcut2>): Byte {
        var sportType2 = ""

        enumValues<SportShortcut2>().forEach {
            sportType2 = if (sportShortcut2.contains(it)) {
                sportType2.plus("1")
            } else {
                sportType2.plus("0")
            }
        }
        return sportType2.reversed().toDecimal().toByte()
    }

    private fun sportShortcut3(sportShortcut3: ArrayList<SportShortcut3>): Byte {
        var sportType3 = ""

        enumValues<SportShortcut3>().forEach {
            sportType3 = if (sportShortcut3.contains(it)) {
                sportType3.plus("1")
            } else {
                sportType3.plus("0")
            }
        }
        return sportType3.reversed().toDecimal().toByte()
    }

    enum class SportShortcut0 {
        WALKING, RUNNING, CYCLING, HIKING, SWIMMING, CLIMBING, BADMINTON, OTHER;
    }

    enum class SportShortcut1 {
        FITNESS, SPINNING, ELLIPTICAL_BALL, TREADMILL, SIT_UPS, PUSH_UPS, DUMBBELLS, WEIGHTLIFTING;
    }

    enum class SportShortcut2 {
        AEROBICS, YOGA, SKIPPING, TABLE_TENNIS, BASKETBALL, FOOTBALL, VOLLEYBALL, TENNIS;
    }

    enum class SportShortcut3 {
        GOLF, BASEBALL, SKIING, SKATING, DANCE;
    }

    fun getMessage(): Message {
        val message = Message()
        message.what = 1
        message.obj = ResponsePackage(R3_SHORTCUT_MODE_SETTING, HashMap(parseData))
        parseData.clear()
        return message
    }

    fun check(byteArray: ByteArray): ResponseStatus {
        return if (byteArray.isNotEmpty() && byteArray[0] != commandId) {
            ResponseStatus.INCOMPATIBLE
        } else if (byteArray.size > 1 && byteArray[1] != keyId) {
            ResponseStatus.INCOMPATIBLE
        } else {
            parseData[0] = byteArray
            ResponseStatus.COMPLETED
        }
    }
}

private var parseData = HashMap<Int, ByteArray>()

internal data class R3ShortcutMode(val data: ByteArray) {
    class Parser : BaseParser {
        private lateinit var model: R3ShortcutMode
        override fun parse(data: HashMap<Int, ByteArray>) {
            println(data.toString())
        }
    }
}