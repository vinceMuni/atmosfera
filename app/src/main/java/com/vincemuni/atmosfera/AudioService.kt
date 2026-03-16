package com.vincemuni.atmosfera

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
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
        const val ACTION_START_TIMER = "com.vincemuni.atmosfera.START_TIMER"
        const val CHANNEL_ID = "AtmosferaChannel"
        const val NOTIF_ID = 1
    }

    private var soundPool: SoundPool? = null
    private var soundId: Int = 0
    private var streamId: Int = 0
    private var timer: Timer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null

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
            ACTION_START_TIMER -> scheduleTimer()
        }
        return START_NOT_STICKY
    }

    private fun playSound() {
        // Must call startForeground() immediately after startForegroundService() - before any other logic
        startForeground(NOTIF_ID, buildNotification())

        // Cleanup previous SoundPool
        timer?.cancel()
        timer = null
        soundPool?.stop(streamId)
        soundPool?.release()
        soundPool = null
        soundId = 0
        streamId = 0

        val soundName = WidgetState.getCurrentSoundName(this).lowercase().replace(" ", "_")
        val resId = resources.getIdentifier(soundName, "raw", packageName)

        if (resId != 0) {
            try {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

                soundPool = SoundPool.Builder()
                    .setMaxStreams(1)
                    .setAudioAttributes(attributes)
                    .build()

                soundPool?.setOnLoadCompleteListener { pool, sampleId, status ->
                    if (status == 0) {
                        val vol = WidgetState.getVolume(this) / 10f
                        streamId = pool.play(sampleId, vol, vol, 1, -1, 1f)
                        if (streamId != 0) {
                            WidgetState.setPlaying(this, true)
                            SoundWidgetProvider.updateAll(this)
                        } else {
                            stopSound(requestStop = false)
                        }
                    } else {
                        stopSound(requestStop = false)
                    }
                }

                soundId = soundPool?.load(this, resId, 1) ?: 0
                if (soundId == 0) {
                    stopSound(requestStop = false)
                }
            } catch (e: Exception) {
                stopSound(requestStop = false)
            }
        } else {
            stopSound(requestStop = false)
        }
    }

    private fun stopSound(requestStop: Boolean = true) {
        timer?.cancel()
        timer = null
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null
        WidgetState.setRemainingMinutes(this, -1)
        soundPool?.stop(streamId)
        soundPool?.release()
        soundPool = null
        soundId = 0
        streamId = 0
        WidgetState.setPlaying(this, false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (requestStop) stopSelf()
        SoundWidgetProvider.updateAll(this)
    }

    private fun adjustVolume(delta: Int) {
        val newVol = WidgetState.getVolume(this) + delta
        WidgetState.setVolume(this, newVol)
        val vol = WidgetState.getVolume(this) / 10f
        if (streamId != 0) soundPool?.setVolume(streamId, vol, vol)
        SoundWidgetProvider.updateAll(this)
    }

    private fun scheduleTimer() {
        val timerIdx = WidgetState.getTimerIndex(this)
        val minutes = WidgetState.TIMER_OPTIONS.getOrElse(timerIdx) { 0 }
        if (minutes > 0) {
            WidgetState.setRemainingMinutes(this, minutes)
            SoundWidgetProvider.updateAll(this)
            startCountdown(minutes)
            timer = Timer()
            timer?.schedule(object : TimerTask() {
                override fun run() {
                    stopSound(requestStop = false)
                    stopSelf()
                }
            }, minutes * 60 * 1000L)
        }
    }

    private fun startCountdown(totalMinutes: Int) {
        countdownRunnable?.let { handler.removeCallbacks(it) }
        var remaining = totalMinutes - 1
        fun tick() {
            if (remaining > 0) {
                WidgetState.setRemainingMinutes(this, remaining)
                SoundWidgetProvider.updateAll(this)
                remaining--
                countdownRunnable = Runnable { tick() }
                handler.postDelayed(countdownRunnable!!, 60_000L)
            }
        }
        countdownRunnable = Runnable { tick() }
        handler.postDelayed(countdownRunnable!!, 60_000L)
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
