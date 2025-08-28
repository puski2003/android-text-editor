package com.example.texteditor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File
import android.os.Environment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.example.texteditor.R
@Composable
fun DirectoryPickerDialog(
    isVisible: Boolean,
    onDirectorySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
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
                            text = "Select Directory",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Quick access directories
                    Text(
                        text = "Quick Access:",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn {
                        items(getQuickAccessDirectories()) { directory ->
                            DirectoryItem(
                                directory = directory,
                                onClick = {
                                    onDirectorySelected(directory.absolutePath)
                                    onDismiss()
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // Create and select TextEditor directory in Documents
                                val textEditorDir = File(
                                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                                    "TextEditor"
                                )
                                if (!textEditorDir.exists()) {
                                    textEditorDir.mkdirs()
                                }
                                onDirectorySelected(textEditorDir.absolutePath)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Create TextEditor Folder")
                        }
                        
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DirectoryItem(
    directory: File,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF383838)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.outline_folder_24),
                contentDescription = null,
                tint = Color(0xFFDCB67A),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = directory.name.ifEmpty { directory.absolutePath },
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = directory.absolutePath,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun getQuickAccessDirectories(): List<File> {
    val directories = mutableListOf<File>()
    
    try {
        // Documents
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (documentsDir.exists()) {
            directories.add(documentsDir)
        }
        
        // Downloads
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloadsDir.exists()) {
            directories.add(downloadsDir)
        }
        
        // External Storage Root
        val externalDir = Environment.getExternalStorageDirectory()
        if (externalDir.exists()) {
            directories.add(externalDir)
        }
        
        // TextEditor directory if it exists
        val textEditorDir = File(documentsDir, "TextEditor")
        if (textEditorDir.exists()) {
            directories.add(0, textEditorDir) // Add at the beginning
        }
        
    } catch (e: Exception) {
        // Handle exceptions silently
    }
    
    return directories
}