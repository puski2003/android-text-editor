package com.example.texteditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle  // Correct import
import kotlinx.coroutines.delay

@Composable
fun TerminalOutputPanel(
    output: String?,
    isCompiling: Boolean,
    onDismiss: () -> Unit,  // Keep this parameter for compatibility but don't use it
    onClearOutput: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Column {
            // Header - remove the close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Terminal",
                        tint = Color.Green,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Output Terminal",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    if (isCompiling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.Green
                        )
                        Text(
                            text = "Compiling...",
                            color = Color.Yellow,
                            fontSize = 12.sp
                        )
                    }
                }
                
                Row {
                    // Clear button
                    IconButton(
                        onClick = onClearOutput,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Output",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    // Close button - add this back
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Terminal",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Divider(color = Color.Gray, thickness = 1.dp)
            
            // Terminal content with colored output
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0C0C0C)) // Darker background for content
                    .padding(12.dp)
            ) {
                val scrollState = rememberScrollState()
                
                LaunchedEffect(output) {
                    // Auto-scroll to bottom when new output arrives
                    if (output?.isNotEmpty() == true) {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }
                
                if (output?.isNotEmpty() == true) {
                    // Color-code different sections of output
                    val styledOutput = buildAnnotatedString {
                        val lines = output.split("\n")
                        lines.forEachIndexed { index, line ->
                            when {
                                line.startsWith("--- Compilation Errors ---") -> {
                                    withStyle(SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)) {
                                        append(line)
                                    }
                                }
                                line.startsWith("--- Warnings ---") -> {
                                    withStyle(SpanStyle(color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)) {
                                        append(line)
                                    }
                                }
                                line.startsWith("--- Execution Output ---") -> {
                                    withStyle(SpanStyle(color = Color.Green, fontWeight = FontWeight.Bold)) {
                                        append(line)
                                    }
                                }
                                line.startsWith("--- Execution Error ---") -> {
                                    withStyle(SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)) {
                                        append(line)
                                    }
                                }
                                line.startsWith("Error:") || line.contains("Exception") -> {
                                    withStyle(SpanStyle(color = Color.Red)) {
                                        append(line)
                                    }
                                }
                                line.startsWith("Compilation successful") -> {
                                    withStyle(SpanStyle(color = Color.Green)) {
                                        append(line)
                                    }
                                }
                                else -> {
                                    withStyle(SpanStyle(color = Color.White)) {
                                        append(line)
                                    }
                                }
                            }
                            if (index < lines.size - 1) {
                                append("\n")
                            }
                        }
                    }
                    
                    Text(
                        text = styledOutput,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                    )
                } else if (isCompiling) {
                    // Animated compiling message
                    var dots by remember { mutableStateOf("") }
                    
                    LaunchedEffect(isCompiling) {
                        while (isCompiling) {
                            dots = when (dots) {
                                "" -> "."
                                "." -> ".."
                                ".." -> "..."
                                else -> ""
                            }
                            delay(500) // Update every 500ms
                        }
                    }
                    
                    Column {
                        Text(
                            text = "> Compiling and executing code$dots",
                            color = Color.Yellow,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "> Please wait...",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Text(
                        text = "> Ready. Press 'Compile & Run' to execute your code.",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}