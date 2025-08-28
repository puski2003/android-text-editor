package com.example.texteditor.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import com.example.texteditor.R
import com.example.texteditor.syntax.SyntaxHighlighter
import com.example.texteditor.syntax.SyntaxConfigLoader
@Composable
fun CodeEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    currentFile: String?,
    currentFilePath: String?,
    isFileExplorerVisible: Boolean, // Add this parameter
    onToggleFileExplorer: () -> Unit,
    onNewFile: () -> Unit,
    onSaveFile: () -> Unit,
    onSaveAsFile: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onShowFindReplace: () -> Unit,
    onCompile: () -> Unit,
    isCompiling: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Load syntax configuration
    val syntaxConfig = remember {
        SyntaxConfigLoader(context).loadConfiguration()
    }
    
    val syntaxHighlighter = remember(syntaxConfig) {
        if (syntaxConfig != null) SyntaxHighlighter(syntaxConfig) else null
    }
    
    // Get file extension from current file path
    val fileExtension = remember(currentFilePath, currentFile) {
        val extension = when {
            currentFilePath != null -> {
                val file = java.io.File(currentFilePath)
                val ext = file.extension
                if (ext.isNotEmpty()) ".$ext" else null
            }
            currentFile != null -> {
                // Handle cases where filename might have .txt incorrectly appended
                val fileName = currentFile
                
                // Check if it's a double extension like "hello.kt.txt"
                if (fileName.contains(".") && fileName.endsWith(".txt")) {
                    val withoutTxt = fileName.removeSuffix(".txt")
                    val lastDot = withoutTxt.lastIndexOf('.')
                    if (lastDot != -1) {
                        // Return the original extension before .txt
                        withoutTxt.substring(lastDot)
                    } else {
                        ".txt"
                    }
                } else {
                    // Normal case
                    val lastDot = fileName.lastIndexOf('.')
                    if (lastDot != -1) fileName.substring(lastDot) else null
                }
            }
            else -> null
        }
        Log.d("CodeEditor", "File: $currentFile, Path: $currentFilePath, Detected extension: $extension")
        extension
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
    ) {
        // Top bar with menu button and tab
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.04f),
            color = Color(0xFF2D2D30)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menu button to toggle file explorer
                IconButton(
                    onClick = onToggleFileExplorer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Toggle File Explorer",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Current file tab
                if (currentFile != null) {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(32.dp),
                        color = Color(0xFF1E1E1E),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentFile,
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
        
        // Toolbar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF252526)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .fillMaxHeight(0.05f)
                    .widthIn(min = 200.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onSaveFile, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.outline_save_24),
                        contentDescription = "Save File",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(onClick = onSaveAsFile, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.baseline_save_as_24),
                        contentDescription = "Save As",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = Color.Gray
                )

                // Show clipboard operations only when file explorer is hidden
                if (!isFileExplorerVisible) {
                    // Clipboard operations

                    IconButton(onClick = onUndo, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.outline_undo_24),
                            contentDescription = "Undo",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onRedo, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.outline_redo_24),
                            contentDescription = "Redo",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp),
                        color = Color.Gray
                    )
                    IconButton(onClick = onCut, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.outline_content_cut_24),
                            contentDescription = "Cut",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onCopy, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.baseline_file_copy_24),
                            contentDescription = "Copy",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onPaste, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.baseline_content_paste_24),
                            contentDescription = "Paste",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp),
                        color = Color.Gray
                    )
                    // Compile button (show for all supported languages)
                    val supportedExtensions = listOf("kt", "py", "java", "c", "cpp", "cc", "cxx", "js", "go")
                    val currentExtension = currentFilePath?.substringAfterLast('.', "")?.lowercase()
                    
                    if (currentExtension in supportedExtensions) {
                        IconButton(
                            onClick = onCompile,
                            enabled = !isCompiling,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isCompiling) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Compile & Run",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Find and Replace
                IconButton(onClick = onShowFindReplace, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Find and Replace",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = Color.Gray
                )


            }
        }
        
        // Line numbers and code editor
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E))
        ) {
            // Line numbers
            LineNumbers(
                text = value.text,
                modifier = Modifier
                    .background(Color(0xFF252526))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
            
            // Text editor with syntax highlighting
            SyntaxHighlightedTextField(
                value = value,
                onValueChange = onValueChange,
                syntaxHighlighter = syntaxHighlighter,
                fileExtension = fileExtension,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun SyntaxHighlightedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    syntaxHighlighter: SyntaxHighlighter?,
    fileExtension: String?,
    modifier: Modifier = Modifier
) {
    val highlightedText = remember(value.text, syntaxHighlighter, fileExtension) {
        syntaxHighlighter?.highlightText(value.text, fileExtension) ?: androidx.compose.ui.text.AnnotatedString(value.text)
    }
    
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = TextStyle(
            color = Color(0xFFD4D4D4),
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 20.sp
        ),
        visualTransformation = { _ ->
            androidx.compose.ui.text.input.TransformedText(
                text = highlightedText,
                offsetMapping = androidx.compose.ui.text.input.OffsetMapping.Identity
            )
        }
    )
}

@Composable
fun LineNumbers(
    text: String,
    modifier: Modifier = Modifier
) {
    val lineCount = text.count { it == '\n' } + 1
    
    Column(
        modifier = modifier
    ) {
        repeat(lineCount) { index ->
            Text(
                text = "${index + 1}",
                color = Color(0xFF858585),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.height(20.dp)
            )
        }
    }
}