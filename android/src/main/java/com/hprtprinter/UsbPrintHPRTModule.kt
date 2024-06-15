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

    override fun getName(): String {
        return "UsbPrintHPRTModule"
    }

    @ReactMethod
    override fun getDevices(promise: Promise) {
        try {

            val deviceList: HashMap<String, UsbDevice> = usbManager.deviceList
            val writableArray: WritableArray = Arguments.createArray()
            for (useDevice in deviceList.values) {
              
                val deviceInfo: WritableMap = Arguments.createMap().apply {
                    putString("deviceName", useDevice.deviceName)
                    putInt("deviceId", useDevice.deviceId)
                    putInt("vendorId", useDevice.vendorId)
                    putInt("productId", useDevice.productId)
                    putInt("deviceClass", useDevice.deviceClass)
                    putInt("deviceSubclass", useDevice.deviceSubclass)
                    putInt("productName", useDevice.deviceProtocol)
                    putString("productName", useDevice.productName)
                    putString("productName", useDevice.manufacturerName)
                    putString("productName", useDevice.version)
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
    override fun updateDeviceHasPermission(useDevice : UsbDevice? , hasPermission : Boolean)  {
        try {
            val deviceInfo: WritableMap = Arguments.createMap();
            if(hasPermission && useDevice?.productId !== null){
                Log.d("TAG", "USB permission granted for device: ${useDevice?.productName}")
                deviceInfo.apply {
                    putString("deviceName", useDevice.deviceName)
                    putInt("deviceId", useDevice.deviceId)
                    putInt("vendorId", useDevice.vendorId)
                    putInt("productId", useDevice.productId)
                    putInt("deviceClass", useDevice.deviceClass)
                    putInt("deviceSubclass", useDevice.deviceSubclass)
                    putInt("productName", useDevice.deviceProtocol)
                    putString("productName", useDevice.productName)
                    putString("productName", useDevice.manufacturerName)
                    putString("productName", useDevice.version)
                    putString("serialNumber",  useDevice.serialNumber.toString())
                    putString("interfacePrinter", "USB")
                }
            }

             val HPRTPrinterModule = HprtPrinterModule(reactApplicationContext)
            HPRTPrinterModule.sendEventToJS(reactApplicationContext, "KEY_UPDATE_LIST_DEVICE", deviceInfo)
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
        val usbManager = reactApplicationContext.getSystemService(Context.USB_SERVICE) as UsbManager?

        // Find the USB device based on vendorId and productId
        val usbDevice = usbManager?.deviceList?.values?.find {
            it.vendorId == vendorId && it.productId == productId
        }

        usbDevice?.let { device ->
            // Create a PendingIntent for USB permission request
            val permissionIntent = PendingIntent.getBroadcast(
                reactApplicationContext,
                0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Create an IntentFilter for USB permission broadcast
            val filter = IntentFilter(ACTION_USB_PERMISSION)
            reactApplicationContext.registerReceiver(usbReceiver, filter)

            // Request permission for the USB device
            usbManager.requestPermission(device, permissionIntent)
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    val hasPermision = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    updateDeviceHasPermission(device,hasPermision)
                    // // Unregister the receiver
                    // reactApplicationContext.unregisterReceiver(this)
                }
            }
        }
    }


}
