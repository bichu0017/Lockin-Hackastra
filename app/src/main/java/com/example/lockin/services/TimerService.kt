package com.example.lockin.services

import java.util.Timer
import kotlin.concurrent.fixedRateTimer

class TimerService {
    private var timer: Timer? = null

    fun start(callback: () -> Unit) {
        timer?.cancel()
        timer = fixedRateTimer(period = 2000L) {
            callback()
        }
    }

    fun stop() {
        timer?.cancel()
        timer = null
    }
}
