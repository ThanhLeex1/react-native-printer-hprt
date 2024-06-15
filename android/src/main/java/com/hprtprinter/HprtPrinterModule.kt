package com.hprtprinter



import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule
import print.Print
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
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.io.InputStream

open class HprtPrinterModule(context: ReactApplicationContext) : ReactContextBaseJavaModule(context)  {
     private val executorService = Executors.newSingleThreadExecutor()
    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    override fun getName(): String {
        return "HprtPrinterModule"
    }

    @ReactMethod
    fun openCashDrawer(openMode : Int ,promise : Promise) {
        if (!Print.IsOpened()) {
            promise.reject("Error", "Please connect printer")
        }
        try {
            val result = Print.OpenCashdrawer(openMode);
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("Error", e)
        }
    }

        @ReactMethod
     fun connectDevice(params: ReadableMap, promise: Promise) {
        if(Print.IsOpened()){
            Print.PortClose()
        }
       try {
        val interfacePrinter = params.getString("interface")
        val identifier = params.getString("identifier")

        if (interfacePrinter == "USB") {
            // Tìm thiết bị USB theo 
            val useDevice = usbManager.deviceList.values.find { it.serialNumber == identifier}
                ?: return promise.reject("Error", "DEVICE_NOT_FOUND")
            Log.d("TAG" , "" + useDevice)
            val result = Print.PortOpen(reactApplicationContext, useDevice)
            promise.resolve(result)

        } else if (interfacePrinter == "LAN") {
            val result = Print.PortOpen(reactApplicationContext, "WiFi,$identifier,9100")
            promise.resolve(result)
        } else {
            promise.reject("INVALID_INTERFACE", "interfacePrinter must be either 'USB' or 'LAN'")
        }
    } catch (e: Exception) {
        promise.reject("UNKNOWN_ERROR", "An unknown error occurred: ${e.message}", e)
    }
    }
    @ReactMethod
    private fun getPrintStatus(realTimeItem : Int , promise: Promise) {
        if (!Print.IsOpened()) {
            promise.reject("Error","Please connect printer")
        }
            try {
            val bytes = Print.GetRealTimeStatus(realTimeItem.toByte())
            Log.d("TAG" , "bytes %s" + bytes[0].toInt())
           promise.resolve(bytes[0].toInt())
        } catch (e: java.lang.Exception) {
            promise.reject("Error", "e123123")
        }
    }

    @ReactMethod
    private fun getCashDrawer(promise: Promise) {
        if (!Print.IsOpened()) {
            promise.reject("Error", "Please connect printer")
        }
        try {
            Print.WriteData(byteArrayOf(0x1D, 0x72, 0x02))
            val status = Print.ReadData(2000)
            if (status == null || status.size != 1) {
                promise.reject("Error", "Time out")
                return
            }
            Log.d("CashDrawer", "status" + status[0])
            promise.resolve(status[0].toInt())
        } catch (e: java.lang.Exception) {
            Log.d("CashDrawer", e.toString())
            promise.reject("Error", "Not get status cash drawer")
        }
    }

    @ReactMethod
    private fun getPrinterSN(promise: Promise){
        if (!Print.IsOpened()) {
            promise.reject("Error", "Please connect printer")
        }
        try {
            val printSN = Print.getPrintSN()
            Log.d("printSN", printSN)
            promise.resolve(printSN)
        } catch (e: java.lang.Exception) {
            promise.reject("Error" ,"Not get series number")
        }
    }

     fun sendEventToJS(reactContext: ReactContext, eventName: String, params: Any?) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }


    @ReactMethod
    private fun disConnectDevice(promise: Promise){
        if (!Print.IsOpened()) {
            return 
        }
        try {
            val result = Print.PortClose()
            promise.resolve(result)
        } catch (e: java.lang.Exception) {
            promise.reject("Error" ,"Disconnection failure")
        }
    }

    @ReactMethod
    fun cutPaper(
        request : ReadableMap,
        promise : Promise
    ) {
        try {
            val  cutMode = request.getInt("cutMode")
            val  distance = request.getInt("distance")
            val  result = Print.CutPaper(cutMode, distance)
             promise.resolve(result)
        }catch (e: java.lang.Exception) {
            promise.reject("Error", e)
        }
    }


    @ReactMethod
    fun printImage(
        request : ReadableMap,
        promise : Promise
    ) {
        val  src = request.getString("source");
        val  isBase64 = request.getBoolean("isBase64");
        val  size = request.getInt("size");
        val  isRotate = request.getBoolean("isRotate");
        val  isLzo = request.getBoolean("isLzo");

        executorService.execute(Runnable {
            var bitmapPrint = src?.let { getBitmapFromURL(it,isBase64 
                ) }
            if( bitmapPrint == null) {
                promise.reject("Error" , "Not get Bitmap from URL")
                return@Runnable
            }
            if (isRotate) bitmapPrint = Utility.Tobitmap90(bitmapPrint)
            if (size != 0) bitmapPrint = Utility.Tobitmap(
                bitmapPrint,
                size,
                Utility.getHeight(size, bitmapPrint!!.getWidth(), bitmapPrint.getHeight())
            )
            var printImage = 0
            try {
                printImage = if (!isLzo) Print.PrintBitmap(
                    bitmapPrint,
                    0,
                    0
                ) else Print.PrintBitmapLZO(bitmapPrint, 0, 0)
                promise.resolve(printImage)
            } catch (e: java.lang.Exception) {
                promise.reject("Error", e)
            }
            bitmapPrint!!.recycle()
        })
    }

     private fun getBitmapFromURL(src: String, isBase64: Boolean): Bitmap? {
        return try {
            if(isBase64){
                val decodedBytes = Base64.decode(src, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } else {
                val url = URL(src)
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input: InputStream = connection.inputStream
                BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    @ReactMethod
fun addListener(eventName: String) {
    // Set up any upstream listeners or background tasks as necessary
  
}

@ReactMethod
fun removeListeners(count: Int) {
    // Remove upstream listeners, stop unnecessary background tasks
}

}