package com.example.texteditor.ui.components

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.texteditor.R
import com.example.texteditor.model.FileItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileExplorer(
    files: List<FileItem>,
    onFileClick: (String, String) -> Unit,
    onCreateFile: (String, String?) -> Unit,
    onCreateDirectory: (String, String?) -> Unit,
    onDeleteFile: (String) -> Unit, // Add delete callback
    onOpenDirectory: () -> Unit,
    currentWorkspaceDir: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateDirDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<FileItem?>(null) }
    var selectedItems by remember { mutableStateOf(setOf<String>()) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuItem by remember { mutableStateOf<FileItem?>(null) }
    
    // Track expanded folders to determine context for new files
    var expandedFolders by remember { mutableStateOf(setOf<String>()) }
    var createInFolder by remember { mutableStateOf<String?>(null) }

    // Function to find the deepest expanded folder
    fun findCreateContext(): String? {
        // Find the most recently expanded folder or the deepest one
        return expandedFolders.lastOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFF252526))
            .padding(4.dp)
    ) {
        // Header with title and action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Files",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            Row {
                IconButton(
                    onClick = { 
                        createInFolder = findCreateContext()
                        showCreateFileDialog = true 
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New File",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { 
                        createInFolder = findCreateContext()
                        showCreateDirDialog = true 
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.baseline_create_new_folder_24),
                        contentDescription = "New Directory",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onOpenDirectory,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.outline_folder_open_24),
                        contentDescription = "Open Directory",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Clear selection button if items are selected
                if (selectedItems.isNotEmpty()) {
                    IconButton(
                        onClick = { selectedItems = emptySet() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Selection",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Show context info when creating in a folder
        if (createInFolder != null) {
            val folderName = java.io.File(createInFolder!!).name
            Text(
                text = "Creating in: $folderName",
                color = Color.Yellow,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        // Selection count
        if (selectedItems.isNotEmpty()) {
            Text(
                text = "${selectedItems.size} item(s) selected",
                color = Color.Yellow,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Current workspace path
        if (currentWorkspaceDir != null) {
            Text(
                text = "Workspace: ${java.io.File(currentWorkspaceDir).name}",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        } else {
            Text(
                text = "No workspace selected",
                color = Color.Red,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Divider(color = Color.Gray, thickness = 1.dp)

        // File tree
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(files) { file ->
                FileTreeItem(
                    file = file,
                    onFileClick = onFileClick,
                    onLongPress = { item ->
                        contextMenuItem = item
                        showContextMenu = true
                    },
                    selectedItems = selectedItems,
                    onSelectionChange = { path, isSelected ->
                        selectedItems = if (isSelected) {
                            selectedItems + path
                        } else {
                            selectedItems - path
                        }
                    },
                    expandedFolders = expandedFolders,
                    onExpandedChange = { path, isExpanded ->
                        expandedFolders = if (isExpanded) {
                            expandedFolders + path
                        } else {
                            expandedFolders - path
                        }
                    },
                    level = 0
                )
            }
        }
    }

    // Context Menu Dialog
    if (showContextMenu && contextMenuItem != null) {
        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            title = {
                Text("File Options")
            },
            text = {
                Text("Choose an action for '${contextMenuItem!!.name}'")
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            val item = contextMenuItem!!
                            selectedItems = selectedItems + item.path
                            showContextMenu = false
                            contextMenuItem = null
                        }
                    ) {
                        Text("Select")
                    }

                    TextButton(
                        onClick = {
                            itemToDelete = contextMenuItem
                            showDeleteDialog = true
                            showContextMenu = false
                            contextMenuItem = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showContextMenu = false
                        contextMenuItem = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog && itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text("Confirm Delete")
            },
            text = {
                Text("Are you sure you want to delete '${itemToDelete!!.name}'? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Actually delete the file
                        onDeleteFile(itemToDelete!!.path)
                        selectedItems = selectedItems - itemToDelete!!.path
                        showDeleteDialog = false
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        itemToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create file dialog
    if (showCreateFileDialog) {
        CreateItemDialog(
            title = if (createInFolder != null) "Create New File in ${java.io.File(createInFolder!!).name}" else "Create New File",
            placeholder = "Enter file name (e.g., script.js, readme.md)",
            onConfirm = { fileName ->
                onCreateFile(fileName, createInFolder)
                showCreateFileDialog = false
                createInFolder = null
            },
            onDismiss = { 
                showCreateFileDialog = false
                createInFolder = null
            }
        )
    }

    // Create directory dialog
    if (showCreateDirDialog) {
        CreateItemDialog(
            title = if (createInFolder != null) "Create New Directory in ${java.io.File(createInFolder!!).name}" else "Create New Directory",
            placeholder = "Enter directory name",
            onConfirm = { dirName ->
                onCreateDirectory(dirName, createInFolder)
                showCreateDirDialog = false
                createInFolder = null
            },
            onDismiss = { 
                showCreateDirDialog = false
                createInFolder = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileTreeItem(
    file: FileItem,
    onFileClick: (String, String) -> Unit,
    onLongPress: (FileItem) -> Unit,
    selectedItems: Set<String>,
    onSelectionChange: (String, Boolean) -> Unit,
    expandedFolders: Set<String>,
    onExpandedChange: (String, Boolean) -> Unit,
    level: Int
) {
    val context = LocalContext.current
    val isExpanded = expandedFolders.contains(file.path)
    val isSelected = selectedItems.contains(file.path)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) Color(0xFF094771) else Color.Transparent
                )
                .combinedClickable(
                    onClick = {
                        if (selectedItems.isNotEmpty()) {
                            // If in selection mode, toggle selection
                            onSelectionChange(file.path, !isSelected)
                        } else {
                            // Normal click behavior
                            if (file.isDirectory) {
                                onExpandedChange(file.path, !isExpanded)
                            } else {
                                onFileClick(file.name, file.path)
                            }
                        }
                    },
                    onLongClick = {
                        onLongPress(file)
                    }
                )
                .padding(start = (level * 16).dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (file.isDirectory) {
                Icon(
                    imageVector = if (isExpanded) ImageVector.vectorResource(id = R.drawable.outline_expand_content_24) else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }
            
            Icon(
                imageVector = if (file.isDirectory) {
                    if (isExpanded) ImageVector.vectorResource(id = R.drawable.outline_folder_open_24) else ImageVector.vectorResource(id = R.drawable.outline_folder_24)
                } else {
                    getFileIcon(file.name)
                },
                contentDescription = null,
                tint = if (file.isDirectory) Color(0xFFDCB67A) else Color(0xFF519ABA),
                modifier = Modifier.size(16.dp)
            )
            
            Text(
                text = file.name,
                color = if (isSelected) Color.White else Color.White,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 6.dp)
            )
            
            // Selection indicator
            if (isSelected) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color.Green,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        if (file.isDirectory && isExpanded) {
            file.children.forEach { child ->
                FileTreeItem(
                    file = child,
                    onFileClick = onFileClick,
                    onLongPress = onLongPress,
                    selectedItems = selectedItems,
                    onSelectionChange = onSelectionChange,
                    expandedFolders = expandedFolders,
                    onExpandedChange = onExpandedChange,
                    level = level + 1
                )
            }
        }
    }
}

@Composable
fun CreateItemDialog(
    title: String,
    placeholder: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var showExtensionSuggestions by remember { mutableStateOf(title.contains("File")) }
    val suggestions = listOf(".txt", ".md", ".js", ".kt", ".java", ".py", ".html", ".css", ".json")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Column {
                Text(
                    text = placeholder,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    placeholder = { Text("Name") },
                    singleLine = true
                )
                
                if (showExtensionSuggestions) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Quick suggestions:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        suggestions.take(3).forEach { ext ->
                            OutlinedButton(
                                onClick = {
                                    val baseName = itemName.substringBeforeLast('.')
                                    itemName = if (baseName.isNotEmpty()) "$baseName$ext" else "untitled$ext"
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(ext, fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        suggestions.drop(3).take(3).forEach { ext ->
                            OutlinedButton(
                                onClick = {
                                    val baseName = itemName.substringBeforeLast('.')
                                    itemName = if (baseName.isNotEmpty()) "$baseName$ext" else "untitled$ext"
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(ext, fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        suggestions.drop(6).forEach { ext ->
                            OutlinedButton(
                                onClick = {
                                    val baseName = itemName.substringBeforeLast('.')
                                    itemName = if (baseName.isNotEmpty()) "$baseName$ext" else "untitled$ext"
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(ext, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (itemName.isNotBlank()) {
                        onConfirm(itemName.trim())
                    }
                },
                enabled = itemName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun getFileIcon(fileName: String): ImageVector {
    return when (fileName.substringAfterLast('.', "").lowercase()) {
        "js", "ts" -> ImageVector.vectorResource(id = R.drawable.baseline_file_copy_24)
        "kt", "java" -> ImageVector.vectorResource(id = R.drawable.baseline_file_copy_24)
        "md", "txt" -> ImageVector.vectorResource(id = R.drawable.baseline_file_copy_24)
        "json", "xml" -> ImageVector.vectorResource(id = R.drawable.baseline_file_copy_24)
        "css", "html" -> ImageVector.vectorResource(id = R.drawable.baseline_file_copy_24)
        else -> ImageVector.vectorResource(id = R.drawable.baseline_file_copy_24)
    }
}