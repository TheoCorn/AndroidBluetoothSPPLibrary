package com.tcorp.cap.bluetooth

enum class BtConnectionState(val state: String) {
    CONNECTED("connected"),
    DISCONNECTED("disconnected"),
    CONNECTING("connecting"),
    NO_BLUETOOTH("no bluetooth available")
}