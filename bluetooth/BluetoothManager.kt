package com.tcorp.cap.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.NonCancellable.isActive
import java.nio.charset.Charset
import java.sql.Time
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.Exception
import kotlin.collections.HashMap


private const val BLUETOOTH_SPP = "00001101-0000-1000-8000-00805F9B34FB"
private const val TAG = "BluetoothManager"

/**
 * An easy way to create and manage bluetooth SPP sockets
 * @param context application Context
 * @param btDevice a BluetoothDevice you want to connect
 * @param target sets a class where it will create a BTListener (or you can set a target by setTargetFragment ToDo
 */
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class BluetoothManager(
    private val context: Context,
    private val btDevice: BluetoothDevice,
    var target: Context,
    startInputStream: Boolean = true
) {

    /**
     * listener for BluetoothManager
     */
    interface BTListener {
        fun onConnect(btDevice: BluetoothDevice)
        fun onDisconnect(btDevice: BluetoothDevice)
        fun onRead(data: String, btDevice: BluetoothDevice)
        fun onConnectionStarted(BtDevice: BluetoothDevice)
        fun onConnectionError(btDevice: BluetoothDevice)
        fun onNoBluetooth()

    }


    var state = BtConnectionState.DISCONNECTED

    private var btsocket: BluetoothSocket? = null

    private var btListener: BTListener? = null

    private var openInputStream: Job? = null
    private var periodicCheckerJob: Job? = null


    init {
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            Log.d(TAG, "target is: $target")
            btListener = target as BTListener
            CoroutineScope(IO).launch { connectToDevice(btDevice, startInputStream) }
        } else {
            state = BtConnectionState.NO_BLUETOOTH
        }


    }

    @InternalCoroutinesApi
    private suspend fun connectToDevice(device: BluetoothDevice, startInputStream: Boolean) {
        state = BtConnectionState.CONNECTING
        CoroutineScope(Main).launch { btListener?.onConnectionStarted(device) }


        try {
            withTimeout(10000) {

                val socket = device.createRfcommSocketToServiceRecord(UUID.fromString(BLUETOOTH_SPP))
                socket.connect()

                btsocket = socket

                state = BtConnectionState.CONNECTED
                CoroutineScope(Main).launch { btListener?.onConnect(device) }


                if (startInputStream) {
                    openInputStream = CoroutineScope(Default).launch { startInputStreamForSocket(socket) }
                } else {
                    periodicCheckerJob = CoroutineScope(IO).launch { periodicCheckForConnection() }
                }
            }

        } catch (e: Exception) {
            state = BtConnectionState.DISCONNECTED
            CoroutineScope(Main).launch { btListener?.onConnectionError(device) }
        }

    }


    @ExperimentalCoroutinesApi
    private suspend fun startInputStreamForSocket(socket: BluetoothSocket) =
        Dispatchers.Unconfined {
            periodicCheckerJob?.cancel()
            try {
                val buffer = ByteArray(1024)
                var len: Int?
                while (isActive) {
                    len = socket.inputStream.read(buffer)
                    if (len != 0) {
                        val data = String(buffer.copyOf(len))
                        Log.d(TAG, "data from socket is: $data")
                        CoroutineScope(Main).launch { btListener?.onRead(data, btDevice) }
                    }
                }
            } catch (ex: Exception) {
                try {
                    socket.close()
                } catch (ex: Exception) {
                } finally {
                    btsocket = null

                    CoroutineScope(Main).launch { btListener?.onDisconnect(btDevice) }
                }
            }
        }

    //write is an kotlin coroutine thus can not be called from java use runWrite()
    /**
     * Dispatcher IO best practice
     * @param data is a String that will be send via BT
     */
    @ExperimentalCoroutinesApi
    suspend fun write(data: String) = Dispatchers.Default {

        if (btsocket?.isConnected == true) {
            btsocket?.outputStream?.write(data.toByteArray(Charset.defaultCharset()))
        }
    }

    /**
     * Dispatcher IO best practice
     * @param data is a ByteArray that will be send via BT
     */
    @ExperimentalCoroutinesApi
    suspend fun write(data: ByteArray) = Dispatchers.Default {
        if (btsocket?.isConnected == true) {
            btsocket?.outputStream?.write(data) ?: Log.d(TAG, "socket not connected in write" )
        }else{
            Log.d(TAG, "socket not connected in write" )
        }
    }

    /**
     * Dispatcher IO best practice
     * @param data is a Int that will be send via BT
     */
    @ExperimentalCoroutinesApi
    suspend fun write(data: Int) = Dispatchers.IO {

        if (btsocket?.isConnected == true) {
            btsocket?.outputStream?.write(data)
        }
    }

    /**
     * Dispatcher IO best practice
     * @param data is a char that will be send via BT
     */
    @ExperimentalCoroutinesApi
    suspend fun write(data: Char) = Dispatchers.IO {

        if (btsocket?.isConnected == true) {
            btsocket?.outputStream?.write(byteArrayOf(data.toByte()))
        }
    }

    /**
     * Dispatcher IO best practice
     * @param data is a mutableList<*> that will be send via BT
     */
    @ExperimentalCoroutinesApi
    suspend fun write(data: Byte) = Dispatchers.IO {

        if (btsocket?.isConnected == true) {
            btsocket?.outputStream?.write(byteArrayOf(data))
        }
    }

    /**
     * Dispatcher IO best practice
     * @param data is a mutableList<*> that will be send via BT
     */
    @ExperimentalCoroutinesApi
    suspend fun write(data: MutableList<*>) = Dispatchers.IO {

        if (btsocket?.isConnected == true) {
            for (d in data) {
                btsocket?.outputStream?.write(d.toString().toByteArray())
            }
        }
    }

    /**
     * Dispatcher IO best practice
     * @param data is a List<*> that will be send via BT
     */
    @JvmName("write1")
    @ExperimentalCoroutinesApi
    suspend fun write(data: List<*>) = Dispatchers.IO {

        if (btsocket?.isConnected == true) {
            for (d in data) {
                btsocket?.outputStream?.write(d.toString().toByteArray())
            }
        }
    }


    /**
     * not the most efficient way to write
     * use only if calling from java (or if you are lazy)
     * @param data Generic allowed types: ByteArray, Byte, String, Int, Char, MutableList<any>
     *
     */
    fun <A> runWrite(data: A) {
        CoroutineScope(IO).launch {
            when (data) {
                is String -> write(data)
                is Int -> write(data)
                is Char -> write(data)
                is ByteArray -> write(data)
                is Byte -> write(data)
                is MutableList<*> -> write(data)
                is List<*> -> write(data)
            }
        }

    }


    suspend fun disconnect() = Dispatchers.IO {
        try {
            btsocket?.close()

        } finally {
            btsocket = null
        }
    }

    fun isConnected(): Boolean {
        return btsocket?.isConnected ?: false
    }

    @InternalCoroutinesApi
    private suspend fun periodicCheckForConnection() {
        while (isActive) {
            delay(1500)
            if (!isConnected()) {
                state = BtConnectionState.DISCONNECTED
                CoroutineScope(Main).launch { btListener?.onDisconnect(btDevice) }
                break
            }
        }
    }
}