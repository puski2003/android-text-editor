package com.example.texteditor.syntax

import androidx.compose.ui.graphics.Color

data class SyntaxTheme(
    val keywords: Color = Color(0xFF569CD6),      // Blue
    val strings: Color = Color(0xFFCE9178),       // Orange
    val comments: Color = Color(0xFF6A9955),      // Green
    val numbers: Color = Color(0xFFB5CEA8),       // Light green
    val operators: Color = Color(0xFFD4D4D4),     // Light gray
    val functions: Color = Color(0xFFDCDCAA),     // Yellow
    val types: Color = Color(0xFF4EC9B0),         // Cyan
    val default: Color = Color(0xFFD4D4D4)        // Default text color
)

data class LanguageRule(
    val name: String,
    val fileExtensions: List<String>,
    val keywords: List<String>,
    val commentStart: String?,
    val commentEnd: String?,
    val lineComment: String?,
    val stringDelimiters: List<String>,
    val numberPattern: String,
    val operatorPattern: String,
    val functionPattern: String,
    val typePattern: String
)

data class SyntaxConfiguration(
    val version: String,
    val theme: SyntaxTheme,
    val languages: List<LanguageRule>
)