package com.example.lockin.services

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

class ScoreManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("score_prefs", Context.MODE_PRIVATE)
    
    private val KEY_SCORE = "daily_score"
    private val KEY_LAST_DATE = "last_reset_date"

    init {
        // We still keep the daily reset logic for multi-day persistence,
        // but the manual reset will be triggered by the UI/Activity on launch.
        checkDailyReset()
    }

    private fun checkDailyReset() {
        val today = getCurrentDate()
        val lastDate = prefs.getString(KEY_LAST_DATE, "")
        
        if (today != lastDate) {
            resetToInitial()
            prefs.edit().putString(KEY_LAST_DATE, today).apply()
        }
    }

    fun resetToInitial() {
        prefs.edit().putInt(KEY_SCORE, 100).apply()
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getScore(): Int {
        return prefs.getInt(KEY_SCORE, 100)
    }

    fun deductPoints(points: Int) {
        val currentScore = getScore()
        val newScore = (currentScore - points).coerceAtLeast(0)
        prefs.edit().putInt(KEY_SCORE, newScore).apply()
    }

    fun getClassification(score: Int): String {
        return when {
            score > 70 -> "Not Addictive / Good"
            score in 40..70 -> "Slightly Addictive"
            else -> "Very Addictive"
        }
    }
}
