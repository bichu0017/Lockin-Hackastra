package com.example.lockin.core

class InterventionController {
    fun action(state: String, mode: String): String {
        if (mode == "Deep Work") {
            if (state != "Productive") return "BLOCK"
        }

        if (mode == "Strict") {
            if (state == "Distracted") return "BLOCK"
            if (state == "Risk Zone") return "WARN"
        }

        if (state == "Distracted") return "WARN"

        return "NONE"
    }
}
