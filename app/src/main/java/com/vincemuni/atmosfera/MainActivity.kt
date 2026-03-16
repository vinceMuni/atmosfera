package com.vincemuni.atmosfera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* granted or denied; notifications work either way on older APIs */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val container = findViewById<LinearLayout>(R.id.sound_list_container)
        val selectedSounds = WidgetState.getSelectedSounds(this).toMutableSet()

        WidgetState.ALL_SOUNDS.forEach { sound ->
            val cb = CheckBox(this).apply {
                text = sound
                isChecked = sound in selectedSounds
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedSounds.add(sound) else selectedSounds.remove(sound)
                    WidgetState.setSelectedSounds(this@MainActivity, selectedSounds.toList())
                    // Reset index if it is now out of bounds
                    val idx = WidgetState.getSoundIndex(this@MainActivity)
                    val newSounds = WidgetState.getSelectedSounds(this@MainActivity)
                    if (newSounds.isNotEmpty() && idx >= newSounds.size) {
                        WidgetState.setSoundIndex(this@MainActivity, 0)
                    }
                    SoundWidgetProvider.updateAll(this@MainActivity)
                }
            }
            container.addView(cb)
        }
    }
}
