package com.example.lockin.services

import android.view.accessibility.AccessibilityEvent
import android.accessibilityservice.AccessibilityService
import android.util.Log

class ScrollAccessibilityService : AccessibilityService() {

    companion object {
        private val scrollTimestamps = mutableListOf<Long>()
        private var lastScrollTime = 0L

        @Volatile
        var isRapidScrollingDetected = false
            private set

        fun resetRapidScrolling() {
            Log.d("ScrollService", "Resetting rapid scrolling")
            isRapidScrollingDetected = false
            scrollTimestamps.clear()
        }

        fun checkRapidScrolling(): Boolean {
            return isRapidScrollingDetected
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Log all events to verify the service is receiving them
        // Log.d("ScrollService", "Event type: ${AccessibilityEvent.eventTypeToString(event.eventType)}")

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            val now = System.currentTimeMillis()
            
            // Reduced debounce to 50ms to be more sensitive
            if (now - lastScrollTime > 50) {
                scrollTimestamps.add(now)
                lastScrollTime = now
                Log.d("ScrollService", "Scroll detected! Total in window: ${scrollTimestamps.size}")
            }

            // Keep only timestamps within the last 20 seconds
            scrollTimestamps.removeAll { now - it > 20000 }

            if (scrollTimestamps.size >= 7) {
                if (!isRapidScrollingDetected) {
                    isRapidScrollingDetected = true
                    Log.d("ScrollService", "!!! DOOMSCROLL DETECTED !!! (7 scrolls in 20s)")
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d("ScrollService", "Service Interrupted")
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("ScrollService", "Accessibility Service Connected and Configured")
    }
}
