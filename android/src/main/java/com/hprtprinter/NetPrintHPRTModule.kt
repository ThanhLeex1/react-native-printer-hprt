package com.hprtprinter


import android.hardware.usb.UsbDevice
import android.text.TextUtils
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.UiThreadUtil.runOnUiThread
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import print.Print
import print.WifiTool
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class NetPrintHPRTModule(context: ReactApplicationContext) : ReactContextBaseJavaModule(context)  , PrinterHelper {
    private val listDevice: WritableArray = Arguments.createArray()

    override fun getName(): String {
        return "NetPrintHPRTModule"
    }

    @ReactMethod
    override fun getDevices(promise: Promise) {
        Thread {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                // Gửi yêu cầu UDP để lấy địa chỉ IP
                val requestMessage = "getval \"printer_message\""
                val wifiIpAddress = WifiTool.getWifiIP(reactApplicationContext)

                if (TextUtils.isEmpty(wifiIpAddress)) {
                    return@Thread
                }

                // Chia địa chỉ IP thành phần mạng và gửi yêu cầu đến mọi thiết bị trong phạm vi mạng
                val ipParts = wifiIpAddress.split("\\.".toRegex()).toTypedArray()
                val networkPrefix = ipParts[0] + "." + ipParts[1] + "." + ipParts[2] + "."
                val broadcastAddress = networkPrefix + "255"

                val requestData = requestMessage.toByteArray()
                val broadcastInetAddress = InetAddress.getByName(broadcastAddress)

                val requestPacket = DatagramPacket(requestData, requestData.size, broadcastInetAddress, 3289)
                socket.send(requestPacket)

                // Nhận phản hồi từ máy tính trong phạm vi mạng
                val responseData = ByteArray(1024)
                val responsePacket = DatagramPacket(responseData, responseData.size)


                socket.receive(responsePacket)


                // Xử lý phản hồi
                val response = String(responsePacket.data, 0, responsePacket.length)
                val responseLines = response.split("\\r\\n".toRegex()).toTypedArray()

                if (responseLines.isNotEmpty()) {
                    getDeviceUdp(responseLines , promise)
                } else {
                    promise.resolve(null)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("TAG", "Exception: " + e.message)
                promise.reject("Error" , e)
            } finally {
                socket?.close()
            }
        }.start()

            }

    override fun updateSerialNumberForDevice(device: UsbDevice) {
        TODO("Not yet implemented")
    }

    private fun getDeviceUdp(data : Array<String> , promise: Promise) {
        Log.d("TAG", "test$data")

        val deviceInfo: WritableMap = Arguments.createMap().apply {
            putString("model",data[5])
            putString("macAddress", data[4])
            putString("serialNumber", data[3])
            putString("status", data[1])
            putString("ipDevice", data[0])
            putString("interfacePrinter", "LAN")
        }

        listDevice.pushMap(deviceInfo)
        promise.resolve(listDevice)
    }
    
}