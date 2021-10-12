package titan.core.products.reflex_3

/**
 * Created by Raviteja Gadipudi.
 * Titan Company Ltd
 */

import android.os.Handler
import titan.core.bluetooth.CommManager
import titan.core.byteMerge
import titan.core.products.*
import titan.core.to32BitByte
import java.util.*

private const val commandId: Byte = -47
private const val startKey: Byte = 1
private const val dataKey: Byte = 2
private const val endKey: Byte = 3
private const val offsetKey: Byte = 4
private const val prnKey: Byte = 5
private const val type: Byte = 0xFF.toByte()
private const val prnValue: Byte = 10
private const val compressionType: Byte = 2

object R3WatchFaces : DataCommand {
    private var listener: DataCallback<Boolean>? = null
    private var dataPacket: ByteArray? = null
    private var currentLength = 0

    fun setWatchFace(dataPacket: ByteArray, name: String): R3WatchFaces {
        this.dataPacket = dataPacket
        currentLength = 0
        val sizeArray = dataPacket.size.to32BitByte()
        println("Size Array-$sizeArray")
        val fileName = if (name.length > 12) name.substring(0, 12) else name
        var data: ByteArray = byteMerge(
            byteArrayOf(
                commandId,
                startKey,
                type,
                sizeArray[3],
                sizeArray[2],
                sizeArray[1],
                sizeArray[0],
                compressionType,
            ), byteMerge(fileName.toByteArray(), byteArrayOf(0,0,0,0,0,0)),
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
        return this
    }

    fun setFastMode() : R3WatchFaces{
        val data: ByteArray = byteArrayOf(
            0x3.toByte(),
            0x35.toByte(),
            1,0,0,0,0,0,0,0,0,0
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
        listener?.onResult(Response.Status(true))
        return this
    }

    fun setSlowMode() : R3WatchFaces{
        val data: ByteArray = byteArrayOf(
            0x3.toByte(),
            0x35.toByte(),
            0,0,0,0,0,0,0,0,0,0
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
        listener?.onResult(Response.Status(true))
        return this
    }


    fun deleteWatchFace(): R3WatchFaces {
        CommManager.getInstance().executeCommand {
            TaskPackage(
                write = Pair(
                    R3_COMMUNICATION_SERVICE,
                    R3_COMMUNICATION_WRITE_CHAR
                ),
                read = null,
                responseWillNotify = true,
                data = byteArrayOf(
                    0x33.toByte(),
                    0xDA.toByte(),
                    0xAD.toByte(),
                    0xDA.toByte(),
                    0xAD.toByte(),
                    0x01.toByte(),
                    0x12.toByte(),
                    0x00.toByte(),
                    0x08.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x00.toByte(),
                    0x02.toByte(),
                ).plus("watch2".toByteArray()),
                key = null
            )
        }
        return this
    }

    private fun setPRN() {
        val data: ByteArray = byteArrayOf(
            commandId,
            prnKey,
            prnValue
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

    private fun transferWatchFace() {
        for (i in 0 until prnValue.toInt()) {
            val size = dataPacket?.size ?: 0
            println("CurrentLength-$currentLength")
            if (currentLength >= size - 1) {
                endTransfer()
                break
            } else {
                val maxLength = when {
                    currentLength + 114 < size -> currentLength + 114
                    else -> size
                }
                println("MaxLength-$maxLength")
                val copyOfCurrentPacket = dataPacket?.copyOfRange(currentLength, maxLength)
                currentLength = maxLength
                if (copyOfCurrentPacket != null) {
                    val data: ByteArray? = byteMerge(
                        byteArrayOf(
                            commandId,
                            dataKey,
                            0.toByte()
                        ), copyOfCurrentPacket
                    )
                    CommManager.getInstance().executeCommand {
                        TaskPackage(
                            write = Pair(
                                R3_COMMUNICATION_SERVICE,
                                R3_COMMUNICATION_WRITE_CHAR
                            ),
                            read = null,
                            responseWillNotify = false,
                            data = data,
                            key = null
                        )
                    }
                }
            }
        }
    }

    private fun endTransfer() {
        val data: ByteArray = byteArrayOf(
            commandId,
            endKey,
            5,
            0x8C.toByte(),
            0x1E.toByte(),
            0
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
        listener?.onResult(Response.Status(true))
    }

    fun callback(callback: DataCallback<Boolean>): R3WatchFaces {
        listener = callback
        return this
    }

    override fun getKey(): UUID? {
        return null
    }

    override fun failed() {
        listener?.onResult(Response.Status(false))
    }

    fun check(byteArray: ByteArray): Boolean {
        return if (byteArray.isNotEmpty() && byteArray[0] != commandId) {
            false
        } else {
            if (byteArray.size > 2 && byteArray[1] == startKey && byteArray[2] == 0.toByte()) {
                println("start key received")
                setPRN()
            } else if (byteArray.size > 1 && byteArray[1] == prnKey) {
                println("prn received")
                Handler().postDelayed({
                    transferWatchFace()
                },1000)
            } else if (byteArray.size > 1 && byteArray[1] == dataKey) {
                println("data key received")
                Handler().postDelayed({
                    transferWatchFace()
                },500)
            }
            true
        }
    }

    fun parse(data: ByteArray) {
        listener?.onResult(
            Response.Status(true)
        )
    }
}
