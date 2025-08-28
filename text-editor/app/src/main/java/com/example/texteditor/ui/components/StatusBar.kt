package com.example.texteditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusBar(
    wordCount: Int,
    charCount: Int,
    currentFile: String?,
    isDirty: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(Color(0xFF007ACC))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = currentFile?.let { "$it${if (isDirty) " •" else ""}" } ?: "No file",
            color = Color.White,
            fontSize = 12.sp
        )
        
        Text(
            text = "Words: $wordCount | Characters: $charCount",
            color = Color.White,
            fontSize = 12.sp
        )
    }
}