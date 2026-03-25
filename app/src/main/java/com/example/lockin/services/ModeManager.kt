package com.example.lockin.services

class ModeManager {
    var currentMode: String = "Normal"
        private set

    fun setMode(mode: String) {
        currentMode = mode
    }
}
