package com.vincemuni.atmosfera

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class SoundWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PREV = "com.vincemuni.atmosfera.PREV"
        const val ACTION_NEXT = "com.vincemuni.atmosfera.NEXT"
        const val ACTION_PLAY_STOP = "com.vincemuni.atmosfera.PLAY_STOP"
        const val ACTION_TIMER = "com.vincemuni.atmosfera.TIMER"
        const val ACTION_VOL_UP = "com.vincemuni.atmosfera.WIDGET_VOL_UP"
        const val ACTION_VOL_DOWN = "com.vincemuni.atmosfera.WIDGET_VOL_DOWN"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, SoundWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                SoundWidgetProvider().onUpdate(context, manager, ids)
            }
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateWidget(context, manager, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PREV -> {
                val sounds = WidgetState.getSelectedSounds(context)
                if (sounds.isNotEmpty()) {
                    var idx = WidgetState.getSoundIndex(context) - 1
                    if (idx < 0) idx = sounds.size - 1
                    WidgetState.setSoundIndex(context, idx)
                    if (WidgetState.isPlaying(context)) {
                        context.startService(
                            Intent(context, AudioService::class.java).apply {
                                action = AudioService.ACTION_STOP
                            }
                        )
                    }
                    updateAll(context)
                }
            }
            ACTION_NEXT -> {
                val sounds = WidgetState.getSelectedSounds(context)
                if (sounds.isNotEmpty()) {
                    val idx = (WidgetState.getSoundIndex(context) + 1) % sounds.size
                    WidgetState.setSoundIndex(context, idx)
                    if (WidgetState.isPlaying(context)) {
                        context.startService(
                            Intent(context, AudioService::class.java).apply {
                                action = AudioService.ACTION_STOP
                            }
                        )
                    }
                    updateAll(context)
                }
            }
            ACTION_PLAY_STOP -> {
                val serviceIntent = Intent(context, AudioService::class.java)
                serviceIntent.action = if (WidgetState.isPlaying(context)) {
                    AudioService.ACTION_STOP
                } else {
                    AudioService.ACTION_PLAY
                }
                context.startService(serviceIntent)
            }
            ACTION_TIMER -> {
                val nextIdx = (WidgetState.getTimerIndex(context) + 1) % WidgetState.TIMER_OPTIONS.size
                WidgetState.setTimerIndex(context, nextIdx)
                updateAll(context)
            }
            ACTION_VOL_UP -> {
                if (WidgetState.isPlaying(context)) {
                    context.startService(
                        Intent(context, AudioService::class.java).apply {
                            action = AudioService.ACTION_VOL_UP
                        }
                    )
                } else {
                    WidgetState.setVolume(context, WidgetState.getVolume(context) + 1)
                    updateAll(context)
                }
            }
            ACTION_VOL_DOWN -> {
                if (WidgetState.isPlaying(context)) {
                    context.startService(
                        Intent(context, AudioService::class.java).apply {
                            action = AudioService.ACTION_VOL_DOWN
                        }
                    )
                } else {
                    WidgetState.setVolume(context, WidgetState.getVolume(context) - 1)
                    updateAll(context)
                }
            }
        }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_sound)

        // Top row
        views.setTextViewText(R.id.tv_sound_name, WidgetState.getCurrentSoundName(context))
        views.setOnClickPendingIntent(R.id.btn_prev, buildBroadcastPI(context, ACTION_PREV, 1))
        views.setOnClickPendingIntent(R.id.btn_next, buildBroadcastPI(context, ACTION_NEXT, 2))

        // Bottom row
        val playStopText = if (WidgetState.isPlaying(context)) "⏹" else "▶"
        views.setTextViewText(R.id.btn_play_stop, playStopText)
        views.setOnClickPendingIntent(R.id.btn_play_stop, buildBroadcastPI(context, ACTION_PLAY_STOP, 3))

        views.setTextViewText(R.id.btn_timer, WidgetState.getTimerLabel(context))
        views.setOnClickPendingIntent(R.id.btn_timer, buildBroadcastPI(context, ACTION_TIMER, 4))

        views.setTextViewText(R.id.tv_volume, "${WidgetState.getVolume(context)}")
        views.setOnClickPendingIntent(R.id.btn_vol_down, buildBroadcastPI(context, ACTION_VOL_DOWN, 5))
        views.setOnClickPendingIntent(R.id.btn_vol_up, buildBroadcastPI(context, ACTION_VOL_UP, 6))

        manager.updateAppWidget(widgetId, views)
    }

    private fun buildBroadcastPI(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, SoundWidgetProvider::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
