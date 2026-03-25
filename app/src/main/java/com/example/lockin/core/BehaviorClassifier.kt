package com.example.lockin.core

class BehaviorClassifier {
    fun classify(score: Double): String {
        return when {
            score >= 70 -> "Productive"
            score >= 40 -> "Risk Zone"
            else -> "Distracted"
        }
    }
}
