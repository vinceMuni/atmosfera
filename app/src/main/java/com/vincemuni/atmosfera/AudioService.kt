package com.vincemuni.atmosfera

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.*
import androidx.core.app.NotificationCompat
import java.util.Timer
import java.util.TimerTask

class AudioService : Service() {

    companion object {
        const val ACTION_PLAY = "com.vincemuni.atmosfera.PLAY"
        const val ACTION_STOP = "com.vincemuni.atmosfera.STOP"
        const val ACTION_VOL_UP = "com.vincemuni.atmosfera.VOL_UP"
        const val ACTION_VOL_DOWN = "com.vincemuni.atmosfera.VOL_DOWN"
        const val CHANNEL_ID = "AtmosferaChannel"
        const val NOTIF_ID = 1
    }

    private var mediaPlayer: MediaPlayer? = null
    private var timer: Timer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> playSound()
            ACTION_STOP -> stopSound()
            ACTION_VOL_UP -> adjustVolume(1)
            ACTION_VOL_DOWN -> adjustVolume(-1)
        }
        return START_NOT_STICKY
    }

    private fun playSound() {
        stopSound()
        val soundName = WidgetState.getCurrentSoundName(this).lowercase().replace(" ", "_")
        val resId = resources.getIdentifier(soundName, "raw", packageName)

        if (resId != 0) {
            try {
                mediaPlayer = MediaPlayer.create(this, resId)?.apply {
                    isLooping = true
                    val vol = WidgetState.getVolume(this@AudioService) / 10f
                    setVolume(vol, vol)
                    start()
                }
            } catch (e: Exception) {
                // No audio file available; widget UI still updates
            }
        }

        WidgetState.setPlaying(this, true)
        startForeground(NOTIF_ID, buildNotification())
        scheduleTimer()
        SoundWidgetProvider.updateAll(this)
    }

    private fun stopSound(requestStop: Boolean = true) {
        timer?.cancel()
        timer = null
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        WidgetState.setPlaying(this, false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (requestStop) stopSelf()
        SoundWidgetProvider.updateAll(this)
    }

    private fun adjustVolume(delta: Int) {
        val newVol = WidgetState.getVolume(this) + delta
        WidgetState.setVolume(this, newVol)
        val vol = WidgetState.getVolume(this) / 10f
        mediaPlayer?.setVolume(vol, vol)
        SoundWidgetProvider.updateAll(this)
    }

    private fun scheduleTimer() {
        val timerIdx = WidgetState.getTimerIndex(this)
        val minutes = WidgetState.TIMER_OPTIONS.getOrElse(timerIdx) { 0 }
        if (minutes > 0) {
            timer = Timer()
            timer?.schedule(object : TimerTask() {
                override fun run() {
                    stopSound()
                    stopSelf()
                }
            }, minutes * 60 * 1000L)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, AudioService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPi = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Atmosfera")
            .setContentText(WidgetState.getCurrentSoundName(this))
            .setContentIntent(mainPi)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPi)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Atmosfera Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Sound playback controls" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopSound(requestStop = false)
        super.onDestroy()
    }
}
