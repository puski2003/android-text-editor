package com.example.texteditor.syntax

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.example.texteditor.compiler.CompilerError
import com.example.texteditor.compiler.ErrorSeverity

class SyntaxHighlighter(private val configuration: SyntaxConfiguration) {
    
    private val TAG = "SyntaxHighlighter"
    
    init {
        Log.d(TAG, "SyntaxHighlighter initialized with ${configuration.languages.size} languages")
        configuration.languages.forEach { lang ->
            Log.d(TAG, "Language: ${lang.name}, Extensions: ${lang.fileExtensions}")
        }
    }
    
    fun highlightText(
        text: String, 
        fileExtension: String?, 
        compileErrors: List<CompilerError> = emptyList()
    ): AnnotatedString {
        Log.d(TAG, "=== Starting highlight for extension: '$fileExtension' ===")
        Log.d(TAG, "Text length: ${text.length}")
        Log.d(TAG, "Compile errors: ${compileErrors.size}")
        
        val languageRule = findLanguageRule(fileExtension)
        if (languageRule == null) {
            Log.w(TAG, "No language rule found for extension: $fileExtension")
            return highlightErrors(AnnotatedString(text), text, compileErrors)
        }
        
        Log.d(TAG, "Using language rule: ${languageRule.name}")
        
        val theme = configuration.theme
        
        val syntaxHighlighted = buildAnnotatedString {
            var currentIndex = 0
            val textLength = text.length
            
            while (currentIndex < textLength) {
                val remainingText = text.substring(currentIndex)
                
                // Skip whitespace
                if (remainingText[0].isWhitespace()) {
                    append(remainingText[0])
                    currentIndex++
                    continue
                }
                
                when {
                    // Handle line comments
                    languageRule.lineComment != null && 
                    remainingText.startsWith(languageRule.lineComment!!) -> {
                        val endOfLine = text.indexOf('\n', currentIndex).let { 
                            if (it == -1) textLength else it 
                        }
                        val commentText = text.substring(currentIndex, endOfLine)
                        
                        Log.d(TAG, "Found comment: '$commentText'")
                        withStyle(SpanStyle(color = theme.comments)) {
                            append(commentText)
                        }
                        currentIndex = endOfLine
                    }
                    
                    // Handle strings
                    remainingText[0] == '"' || remainingText[0] == '\'' -> {
                        val delimiter = remainingText[0].toString()
                        val endIndex = remainingText.indexOf(delimiter, 1)
                        val stringText = if (endIndex != -1) {
                            remainingText.substring(0, endIndex + 1)
                        } else {
                            remainingText.substringBefore('\n').ifEmpty { remainingText }
                        }
                        
                        Log.d(TAG, "Found string: '$stringText'")
                        withStyle(SpanStyle(color = theme.strings)) {
                            append(stringText)
                        }
                        currentIndex += stringText.length
                    }
                    
                    // Handle words (potential keywords)
                    remainingText[0].isLetter() || remainingText[0] == '_' -> {
                        val wordMatch = Regex("^[a-zA-Z_]\\w*").find(remainingText)?.value
                        if (wordMatch != null) {
                            val isKeyword = languageRule.keywords.contains(wordMatch)
                            
                            if (isKeyword) {
                                Log.d(TAG, "Found keyword: '$wordMatch'")
                                withStyle(SpanStyle(color = theme.keywords, fontWeight = FontWeight.Bold)) {
                                    append(wordMatch)
                                }
                            } else {
                                withStyle(SpanStyle(color = theme.default)) {
                                    append(wordMatch)
                                }
                            }
                            currentIndex += wordMatch.length
                        } else {
                            append(remainingText[0])
                            currentIndex++
                        }
                    }
                    
                    // Handle numbers
                    remainingText[0].isDigit() -> {
                        val numberMatch = Regex("^\\d+(\\.\\d+)?[fFlL]?").find(remainingText)?.value
                        if (numberMatch != null) {
                            Log.d(TAG, "Found number: '$numberMatch'")
                            withStyle(SpanStyle(color = theme.numbers)) {
                                append(numberMatch)
                            }
                            currentIndex += numberMatch.length
                        } else {
                            append(remainingText[0])
                            currentIndex++
                        }
                    }
                    
                    // Handle operators
                    remainingText[0].toString().matches(Regex("[+\\-*/%=<>!&|^~?:]")) -> {
                        withStyle(SpanStyle(color = theme.operators)) {
                            append(remainingText[0])
                        }
                        currentIndex++
                    }
                    
                    // Default case
                    else -> {
                        withStyle(SpanStyle(color = theme.default)) {
                            append(remainingText[0])
                        }
                        currentIndex++
                    }
                }
            }
        }
        
        // Apply error highlighting on top of syntax highlighting
        return highlightErrors(syntaxHighlighted, text, compileErrors)
    }
    
    private fun highlightErrors(
        annotatedString: AnnotatedString,
        originalText: String,
        errors: List<CompilerError>
    ): AnnotatedString {
        if (errors.isEmpty()) return annotatedString
        
        return buildAnnotatedString {
            append(annotatedString)
            
            val lines = originalText.lines()
            
            errors.forEach { error ->
                // Handle nullable line and column
                val errorLine = error.line
                val errorColumn = error.column
                
                if (errorLine != null && errorLine > 0 && errorLine <= lines.size) {
                    val lineIndex = errorLine - 1
                    var lineStartIndex = 0
                    
                    // Calculate the start index of the error line
                    for (i in 0 until lineIndex) {
                        lineStartIndex += lines[i].length + 1 // +1 for newline
                    }
                    
                    val lineLength = lines[lineIndex].length
                    val columnOffset = (errorColumn ?: 1) - 1 // Use 1 as default if column is null
                    val errorStart = lineStartIndex + columnOffset.coerceIn(0, lineLength)
                    val errorEnd = (errorStart + 1).coerceAtMost(lineStartIndex + lineLength)
                    
                    if (errorStart < originalText.length) {
                        val errorColor = when (error.severity) {
                            "ERROR" -> Color.Red
                            "WARNING" -> Color(0xFFFF9800) // Orange
                            "INFO" -> Color(0xFF2196F3) // Blue
                            else -> Color.Red // Default to red for unknown severity
                        }
                        
                        addStyle(
                            style = SpanStyle(
                                textDecoration = TextDecoration.Underline,
                                color = errorColor
                            ),
                            start = errorStart,
                            end = errorEnd
                        )
                    }
                }
            }
        }
    }
    
    private fun findLanguageRule(fileExtension: String?): LanguageRule? {
        Log.d(TAG, "Looking for language rule for extension: '$fileExtension'")
        
        if (fileExtension == null) {
            Log.d(TAG, "Extension is null")
            return null
        }
        
        val rule = configuration.languages.find { rule ->
            val matches = rule.fileExtensions.any { ext -> 
                val match1 = fileExtension.lowercase().endsWith(ext.lowercase())
                val match2 = fileExtension.lowercase() == ext.lowercase()
                Log.d(TAG, "Checking '$fileExtension' against '$ext': endsWith=$match1, equals=$match2")
                match1 || match2
            }
            matches
        }
        
        Log.d(TAG, "Found rule: ${rule?.name ?: "none"}")
        return rule
    }
}