package com.hprtprinter


import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Base64
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import print.Print
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class UsbPrintHPRTModule(context: ReactApplicationContext) : ReactContextBaseJavaModule(context) , PrinterHelper
   {
    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val executorService = Executors.newSingleThreadExecutor()
    private val ACTION_USB_PERMISSION = "com.PRINTSDK"
     private val HPRTPrinterModule = HprtPrinterModule(reactApplicationContext)

       override fun getName(): String {
        return "UsbPrintHPRTModule"
    }

    @ReactMethod
    override fun getDevices(promise: Promise) {
        try {

            val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
            val writableArray: WritableArray = Arguments.createArray()
            for (useDevice in deviceList.values) {
                var serialNumber =  "Not available"
                if(hasPermissionUSB(useDevice.vendorId ,useDevice.productId , null  ) && useDevice.serialNumber != null ){
                    serialNumber =  useDevice.serialNumber.toString()
                }
                val deviceInfo: WritableMap = Arguments.createMap().apply {
                    putString("deviceName", useDevice.deviceName)
                    putInt("deviceId", useDevice.deviceId)
                    putInt("vendorId", useDevice.vendorId)
                    putInt("productId", useDevice.productId)
                    putInt("deviceClass", useDevice.deviceClass)
                    putInt("deviceSubclass", useDevice.deviceSubclass)
                    putString("productName", useDevice.productName)
                    putString("serialNumber", serialNumber)
                    putString("interfacePrinter", "USB")
                }
                writableArray.pushMap(deviceInfo)
            }
            promise.resolve(writableArray)
        }catch (e: java.lang.Exception) {
            promise.reject("Error", e)
        }

    }

    @ReactMethod
    override fun updateSerialNumberForDevice(device : UsbDevice)  {
        try {
            val result: WritableMap = Arguments.createMap()
            val deviceInfo: WritableMap = Arguments.createMap().apply {
                putInt("deviceId", device.deviceId)
                putString("serialNumber", device.serialNumber.toString())
            }
           result.putMap("eventData", deviceInfo);
           result.putString("eventName", "KEY_UPDATE_LIST_DEVICE");

            HPRTPrinterModule.sendEventToJS(reactApplicationContext, "eventEmiter", result)
           }catch (e: java.lang.Exception) {
               Log.d("Error" , e.toString())
           }
    }

    @ReactMethod
    fun hasPermissionUSB(vendorId: Int, productId: Int, promise: Promise?) : Boolean {
        return try {
            val useDevice =  usbManager.deviceList.values.find { it.vendorId == vendorId && it.productId == productId  }
           val res =  usbManager.hasPermission(useDevice)
            promise?.resolve(res)
            res
        } catch (e: java.lang.Exception) {
            promise?.resolve(e)
         false
        }
    }
    @ReactMethod
    fun requestPermissionUSB(vendorId : Int, productId : Int , promise: Promise) {
        try {
            val useDevice =  usbManager.deviceList.values.find { it.vendorId == vendorId && it.productId == productId  }
            val permissionIntent = PendingIntent.getBroadcast(reactApplicationContext, 0, Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE)
            val filter = IntentFilter(ACTION_USB_PERMISSION)
            reactApplicationContext.registerReceiver(usbReceiver, filter)
            usbManager.requestPermission(useDevice, permissionIntent)
           }catch (e: java.lang.Exception) {
               promise.reject("Error", e)
           }
    }

        private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let {
                            updateSerialNumberForDevice(device)
                        }
                    }
                }
            }
        }
    }

   

    
}