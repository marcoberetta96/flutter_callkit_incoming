package com.hiennv.flutter_callkit_incoming

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.text.TextUtils

class CallkitSoundPlayerService : Service() {

    private var vibrator: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null
    private var data: Bundle? = null
    private val handler: Handler = Handler(Looper.getMainLooper())

    private val runnableTimeout = Runnable {
        val intent = CallkitIncomingBroadcastReceiver.getIntentTimeout(this@CallkitSoundPlayerService, data)
        sendBroadcast(intent)
        stopSelf()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        this.playSound(intent)
        this.playVibrator()
        return START_STICKY;
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnableTimeout)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        vibrator?.cancel()
    }

    private fun playVibrator() {
        vibrator  = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =  this.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0L, 1000L, 1000L), 0))
        }else{
            vibrator?.vibrate(longArrayOf(0L, 1000L, 1000L), 0)
        }
    }

    private fun playSound(intent: Intent?) {
        this.data = intent?.extras
        val sound = this.data?.getString(
            CallkitIncomingBroadcastReceiver.EXTRA_CALLKIT_RINGTONE_PATH,
            ""
        )
        val duration = this.data?.getLong(
            CallkitIncomingBroadcastReceiver.EXTRA_CALLKIT_DURATION,
            30000L
        )
        var uri = sound?.let { getRingtoneUri(it) }
        if(uri == null){
            uri = RingtoneManager.getActualDefaultRingtoneUri(
                this@CallkitSoundPlayerService,
                RingtoneManager.TYPE_RINGTONE
            )
        }
        mediaPlayer = MediaPlayer()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val attribution = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setLegacyStreamType(AudioManager.STREAM_RING)
                .build()
            mediaPlayer?.setAudioAttributes(attribution)
        } else {
            mediaPlayer?.setAudioStreamType(AudioManager.STREAM_RING)
        }
        try {
            mediaPlayer?.setDataSource(applicationContext, uri!!)
            mediaPlayer?.prepare()
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (duration != null) {
            handler.postDelayed(runnableTimeout, duration)
        }
    }


    private fun getRingtoneUri(fileName: String) = try {
        if (TextUtils.isEmpty(fileName)) {
            RingtoneManager.getActualDefaultRingtoneUri(
                this@CallkitSoundPlayerService,
                RingtoneManager.TYPE_RINGTONE
            )
        }
        val resId = resources.getIdentifier(fileName, "raw", packageName)
        if (resId != 0) {
            Uri.parse("android.resource://${packageName}/$resId")
        } else {
            if (fileName.equals("system_ringtone_default", true)) {
                RingtoneManager.getActualDefaultRingtoneUri(
                    this@CallkitSoundPlayerService,
                    RingtoneManager.TYPE_RINGTONE
                )
            } else {
                RingtoneManager.getActualDefaultRingtoneUri(
                    this@CallkitSoundPlayerService,
                    RingtoneManager.TYPE_RINGTONE
                )
            }
        }
    } catch (e: Exception) {
        if (fileName.equals("system_ringtone_default", true)) {
            RingtoneManager.getActualDefaultRingtoneUri(
                this@CallkitSoundPlayerService,
                RingtoneManager.TYPE_RINGTONE
            )
        } else {
            RingtoneManager.getActualDefaultRingtoneUri(
                this@CallkitSoundPlayerService,
                RingtoneManager.TYPE_RINGTONE
            )
        }
    }
}