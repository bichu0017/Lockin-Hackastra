package com.example.lockin.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun FocusIndicator(score: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Focus Score",
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        CircularProgressIndicator(
            progress = { (score / 100).toFloat() },
            strokeWidth = 8.dp,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = String.format(Locale.getDefault(), "%.1f", score))
    }
}
