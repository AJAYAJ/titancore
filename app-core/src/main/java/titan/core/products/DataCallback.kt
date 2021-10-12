package titan.core.products

import java.util.*


/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

interface DataCommand {
    fun getKey(): UUID?
    fun failed()
}

interface DataCallback<T> {
    fun onResult(response: Response<T>)
}

sealed class Response<T> {
    data class Result<T>(val result: T) : Response<T>()
    data class Status<T>(val status: Boolean, val error: String? = null) : Response<T>()
}