package com.tcorp.cap.bluetooth

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tcorp.cap.btimpthread.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi


private const val TAG = "BluetoothService"
@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class BluetoothService : Service() {


    inner class LocalBinder : Binder(){
        fun getService(): BluetoothService = this@BluetoothService
    }

    private val binder = LocalBinder()


    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    private val mapManagerAndDevice = hashMapOf<BluetoothDevice, BluetoothManager>()


    override fun onCreate() {
        super.onCreate()
        val btServiceChannel = NotificationChannel(
            Constants.BLUETOOTH_SERVICE_NOTIFICATION_CHANEL.string,
            Constants.NOTIFICATION_NAME.string,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).also {
            it.createNotificationChannel(btServiceChannel) }
        val notification = NotificationCompat.Builder(this, Constants.BLUETOOTH_SERVICE_NOTIFICATION_CHANEL.string)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(Constants.UMD_IS_RUNNING_BLUETOOTH.string)
            .build()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            if (intent?.getStringExtra(Constants.IntentOnDisconnect.string) == Constants.IntentOnDisconnect.string){
                val name = intent.getStringExtra(Constants.DeviceName.string)
                for (device in mapManagerAndDevice.keys){
                    if (device.name == name) {
                        mapManagerAndDevice.remove(device)
                        Log.d(TAG, "removed device")
                    }
                }
            }
        return START_STICKY
    }

    /**
     * @param btDevice device to connect
     * @param btManager a BluetoothManager object for the device
     */
    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    fun add(btDevice: BluetoothDevice, btManager: BluetoothManager){
        mapManagerAndDevice[btDevice] = btManager
    }

    /**
     * @return List<BluetoothDevice>
     */
    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    fun getConnectedDevices() = mapManagerAndDevice.keys.toList()

    /**
     * @param btDevice BluetoothDevice you want to get a BluetoothManager for
     * @return BluetoothManager for BluetoothDevice
     * returns null if manager doesn't exist
     * to create a BluetoothManager object use add function in BluetoothService
     */
    fun getBluetoothManager(btDevice: BluetoothDevice) = mapManagerAndDevice[btDevice]


    /**
     * returns true if the device is connected
     * @return Boolean
     */
    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    fun connected(btDevice: BluetoothDevice) = mapManagerAndDevice[btDevice]?.isConnected()

    fun state(btDevice: BluetoothDevice) = mapManagerAndDevice[btDevice]?.state

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onDestroy() {
        stopForeground(true)
        stopSelf()
    }

}