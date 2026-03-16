# Lessons Learned

## MediaPlayer Event Handling (2026-03-16)

### Problem
MediaPlayer generating "mediaplayer went away with unhandled events" warnings in logs when widget play button is pressed.

### Root Cause
1. MediaPlayer event listeners (onError, onCompletion, onInfo) were not set, causing Android to report unhandled events
2. MediaPlayer was being released without calling `reset()` first, leaving messages in its internal event queue
3. The DRM cleanup phase during teardown showed pending events still in the queue

### Solution Pattern
Always follow this MediaPlayer lifecycle pattern:

**Creation:**
```kotlin
mediaPlayer = MediaPlayer.create(context, resId)?.apply {
    // Set ALL listeners BEFORE starting playback
    setOnErrorListener { mp, _, _ -> 
        // Handle error, release, return true
        true 
    }
    setOnCompletionListener { mp -> 
        // Handle completion
    }
    setOnInfoListener { _, _, _ -> 
        // Handle info events, return false
        false 
    }
    start()
}
```

**Teardown (Critical!):**
```kotlin
mediaPlayer?.apply {
    try {
        if (isPlaying) stop()
        reset()  // KEY: Clears internal message queue!
    } catch (e: IllegalStateException) {
        // Handle invalid state
    } finally {
        // Clear listeners before release
        setOnErrorListener(null)
        setOnCompletionListener(null)
        setOnInfoListener(null)
        release()
    }
}
```

### Key Takeaways
- **Always set event listeners** even if you think they won't fire (looping, etc.)
- **Always call `reset()` before `release()`** to drain the message queue
- **Clear listeners in finally block** to prevent callbacks during teardown
- **Wrap in try-catch-finally** to handle edge cases when MediaPlayer is in invalid state

### References
- Android MediaPlayer state diagram: https://developer.android.com/reference/android/media/MediaPlayer#state-diagram
- Event handling is required even for looping playback

