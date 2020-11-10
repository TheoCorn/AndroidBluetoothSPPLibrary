package com.tcorp.cap.btimpthread

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.*
import com.tcorp.cap.bluetooth.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main

private const val TAG = "MainActivity"


@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class MainActivity : AppCompatActivity(), BluetoothManager.BTListener {

    private lateinit var bluetoothService: BluetoothService
    private var mBound = false

    private lateinit var conState: TextView
    private lateinit var txt: TextView
    private lateinit var list: ListView
    private lateinit var btnSend: Button
    private lateinit var device: BluetoothDevice

    private val bondedDevices = mutableListOf<BluetoothDevice>()




    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Intent(this, BluetoothService::class.java).also {
            startForegroundService(it)
            bindService(it, connection, Context.BIND_AUTO_CREATE)
        }

        CoroutineScope(Default).launch { checkForConnectedDevices() }


        txt = findViewById(R.id.txt)
        conState = findViewById(R.id.connState)
        list = findViewById(R.id.listView)
        btnSend = findViewById(R.id.btnSend)



        val names = mutableListOf<String>()
        BluetoothAdapter.getDefaultAdapter().bondedDevices.forEach { names.add(it.name); bondedDevices.add(it) }


        val adapter = ArrayAdapter(applicationContext, android.R.layout.simple_list_item_1, names)
        list.adapter = adapter

        list.setOnItemClickListener { _, _, position, _ ->
            device = bondedDevices[position]
            if (!bluetoothService.connected(device)){
                bluetoothService.add(device, BluetoothManager(device, this, this,true))
            }else{
                CoroutineScope(IO).launch { bluetoothService.getBluetoothManager(device)?.disconnect() }
                Log.d(TAG, "disconnect")
            }
        }

        btnSend.setOnClickListener {
            val allConnectedDevices = bluetoothService.getConnectedDevices()
            CoroutineScope(Default).launch { bluetoothService.getBluetoothManager(allConnectedDevices[0])?.write("1") }
        }

    }

    private suspend fun checkForConnectedDevices() {
        while (!this::bluetoothService.isInitialized) {
            delay(100)
        }
        Log.d(TAG, "btService is init")
        bondedDevices.addAll(bluetoothService.getConnectedDevices())
        if (bluetoothService.getConnectedDevices().isNotEmpty()){
            runOnUiThread { conState.text = BtConnectionState.CONNECTED.state }
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, BluetoothService::class.java).also { bindService(it, connection, Context.BIND_AUTO_CREATE) }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
        mBound = false
    }


    override fun onRead(data: String, btDevice: BluetoothDevice) {
        txt.text = data
    }

    override fun onConnectionStarted(BtDevice: BluetoothDevice) {
        conState.text = BtConnectionState.CONNECTING.state
    }

    override fun onConnectionError(btDevice: BluetoothDevice) {
        Toast.makeText(applicationContext, "unable to connect to ${btDevice.name}", Toast.LENGTH_SHORT).show()
        conState.text = BtConnectionState.DISCONNECTED.state
    }

    override fun onNoBluetooth() {
        conState.text = BtConnectionState.NO_BLUETOOTH.state
    }

    override fun onConnect(btDevice: BluetoothDevice){
        Toast.makeText(applicationContext, "Connected to ${btDevice.name}", Toast.LENGTH_SHORT).show()
        conState.text = BtConnectionState.CONNECTED.state
    }

    override fun onDisconnect(btDevice: BluetoothDevice) {
        Toast.makeText(applicationContext, "Disconnected to ${btDevice.name}", Toast.LENGTH_SHORT).show()
        conState.text = BtConnectionState.DISCONNECTED.state
    }


    private val connection = object : ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as BluetoothService.LocalBinder
            bluetoothService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
            Log.d(TAG, "service disconnected")
        }

    }


}