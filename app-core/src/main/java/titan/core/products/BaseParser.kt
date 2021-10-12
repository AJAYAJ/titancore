package titan.core.products

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

interface BaseParser {
    fun parse(data: HashMap<Int, ByteArray>)
}

enum class ResponseStatus {
    COMPLETED,
    INCOMPATIBLE,
    INCOMPLETE,
    INVALID_DATA_LENGTH,
    ITEM_MISSED
}