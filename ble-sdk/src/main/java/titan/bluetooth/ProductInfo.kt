package titan.bluetooth

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

class ProductInfo constructor(private val id: Int, private val code: String) {
    fun getCode(): String {
        return code
    }
}