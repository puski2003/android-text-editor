package com.example.texteditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun FindReplaceDialog(
    isVisible: Boolean,
    findText: String,
    replaceText: String,
    matchCase: Boolean,
    matchWholeWord: Boolean,
    onFindTextChange: (String) -> Unit,
    onReplaceTextChange: (String) -> Unit,
    onMatchCaseToggle: () -> Unit,
    onMatchWholeWordToggle: () -> Unit,
    onFindNext: () -> Unit,
    onReplaceNext: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit
) {
    if (isVisible) {
        Dialog(onDismissRequest = onClose) {
            Surface(
                modifier = Modifier
                    .width(400.dp)
                    .padding(16.dp),
                color = Color(0xFF2D2D30),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Find and Replace",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Find field
                    Text(
                        text = "Find:",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    BasicTextField(
                        value = findText,
                        onValueChange = onFindTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1E1E))
                            .padding(8.dp),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Replace field
                    Text(
                        text = "Replace:",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    BasicTextField(
                        value = replaceText,
                        onValueChange = onReplaceTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1E1E))
                            .padding(8.dp),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = matchCase,
                                onCheckedChange = { onMatchCaseToggle() }
                            )
                            Text(
                                text = "Match case",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = matchWholeWord,
                                onCheckedChange = { onMatchWholeWordToggle() }
                            )
                            Text(
                                text = "Whole word",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Buttons
                    Column (
                        modifier = Modifier.fillMaxWidth(),

                    ) {
                        Button(
                            onClick = onFindNext,

                        ) {
                            Text("Find Next")
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier.fillMaxWidth()

                        ) { Button(
                            onClick = onReplaceNext,


                            ) {
                            Text("Replace")
                        }

                            Button(
                                onClick = onReplaceAll,

                                ) {
                                Text("Replace All")
                            } }
                    }
                }
            }
        }
    }
}