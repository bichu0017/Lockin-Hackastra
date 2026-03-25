package com.example.lockin.core

import com.example.lockin.data.models.ActivityData

class FocusEngine {
    fun calculate(data: ActivityData): Double {
        var score = 100.0

        score -= data.tabSwitches * 4.0
        score -= data.scrollSpeed * 2.0

        if (data.idleTime > 5) {
            score -= 25.0
        }

        // Independent Condition 1: Continuous app usage >= 30 seconds
        if (data.continuousAppUsageSeconds >= 30) {
            return 0.0 
        }

        // Independent Condition 2: Doomscrolling (continuous scrolling >= 15 seconds)
        if (data.continuousScrollSeconds >= 15) {
            return 0.0
        }
        
        // Independent Condition 3: Doomscrolling (count-based - 7 consecutive items)
        if (data.scrollCount >= 7) {
            return 0.0
        }

        return score.coerceIn(0.0, 100.0)
    }
}
