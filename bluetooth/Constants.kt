package com.tcorp.cap.bluetooth

import com.tcorp.cap.btimpthread.BuildConfig

enum class Constants(val string: String) {
    BLUETOOTH_SERVICE_NOTIFICATION_CHANEL("${BuildConfig.APPLICATION_ID}.btServiceNotification"),
    NOTIFICATION_NAME("BLUETOOTH_SERVICE_NOTIFICATION_NAME"),
    UMD_IS_RUNNING_BLUETOOTH("Bluetooth"),
    IntentOnDisconnect("ON_DISCONNECT"),
    DeviceName("DEVICE_NAME")
}