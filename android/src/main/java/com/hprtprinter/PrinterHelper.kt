package com.hprtprinter

import android.hardware.usb.UsbDevice
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap

interface PrinterHelper {
    fun getDevices(promise: Promise)
    fun updateSerialNumberForDevice(device: UsbDevice)
}