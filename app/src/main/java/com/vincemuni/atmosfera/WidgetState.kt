package com.vincemuni.atmosfera

import android.content.Context
import android.content.SharedPreferences

object WidgetState {
    private const val PREFS = "com.vincemuni.atmosfera.widget"
    private const val KEY_SOUND_INDEX = "sound_index"
    private const val KEY_IS_PLAYING = "is_playing"
    private const val KEY_TIMER = "timer_minutes"
    private const val KEY_VOLUME = "volume"
    private const val KEY_SELECTED_SOUNDS = "selected_sounds"
    private const val KEY_REMAINING_MINUTES = "remaining_minutes"

    val ALL_SOUNDS = listOf("Fan", "Hairdryer", "Strong Fan", "Cafe")
    val TIMER_OPTIONS = listOf(0, 15, 30, 60, 90) // 0 = infinite

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getSelectedSounds(context: Context): List<String> {
        val saved = prefs(context).getString(KEY_SELECTED_SOUNDS, null)
        return if (saved.isNullOrEmpty()) ALL_SOUNDS
        else saved.split(",").filter { it.isNotEmpty() }
    }

    fun setSelectedSounds(context: Context, sounds: List<String>) {
        prefs(context).edit().putString(KEY_SELECTED_SOUNDS, sounds.joinToString(",")).apply()
    }

    fun getSoundIndex(context: Context): Int = prefs(context).getInt(KEY_SOUND_INDEX, 0)
    fun setSoundIndex(context: Context, index: Int) {
        prefs(context).edit().putInt(KEY_SOUND_INDEX, index).apply()
    }

    fun isPlaying(context: Context): Boolean = prefs(context).getBoolean(KEY_IS_PLAYING, false)
    fun setPlaying(context: Context, playing: Boolean) {
        prefs(context).edit().putBoolean(KEY_IS_PLAYING, playing).apply()
    }

    fun getTimerIndex(context: Context): Int = prefs(context).getInt(KEY_TIMER, 0)
    fun setTimerIndex(context: Context, index: Int) {
        prefs(context).edit().putInt(KEY_TIMER, index).apply()
    }

    fun getVolume(context: Context): Int = prefs(context).getInt(KEY_VOLUME, 5)
    fun setVolume(context: Context, volume: Int) {
        prefs(context).edit().putInt(KEY_VOLUME, volume.coerceIn(0, 10)).apply()
    }

    fun getCurrentSoundName(context: Context): String {
        val sounds = getSelectedSounds(context)
        if (sounds.isEmpty()) return "—"
        return sounds[getSoundIndex(context).coerceIn(0, sounds.size - 1)]
    }

    fun getRemainingMinutes(context: Context): Int = prefs(context).getInt(KEY_REMAINING_MINUTES, -1)
    fun setRemainingMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_REMAINING_MINUTES, minutes).apply()
    }

    fun getTimerLabel(context: Context): String {
        val remaining = getRemainingMinutes(context)
        if (remaining >= 0) return "⏱ ${remaining}m"
        val idx = getTimerIndex(context)
        val minutes = TIMER_OPTIONS.getOrElse(idx) { 0 }
        return if (minutes == 0) "∞" else "${minutes}m"
    }
}
