package com.example.texteditor.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.texteditor.ui.components.CodeEditor
import com.example.texteditor.ui.components.FileExplorer
import com.example.texteditor.ui.components.FindReplaceDialog
import com.example.texteditor.ui.components.SaveAsDialog
import com.example.texteditor.ui.components.StatusBar
import com.example.texteditor.ui.components.TerminalOutputPanel
import com.example.texteditor.viewmodel.TextEditorViewModel
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.documentfile.provider.DocumentFile
import kotlin.math.abs

@Composable
fun TextEditorScreen(
    viewModel: TextEditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val textFieldValue by viewModel.textFieldValue.collectAsState()
    val currentFile by viewModel.currentFile.collectAsState()
    val files by viewModel.files.collectAsState()
    val isFileExplorerVisible by viewModel.isFileExplorerVisible.collectAsState()
    val isDirty by viewModel.isDirty.collectAsState()
    val wordCount by viewModel.wordCount.collectAsState()
    val charCount by viewModel.charCount.collectAsState()
    val currentWorkspaceDir by viewModel.currentWorkspaceDir.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    
    // Find and Replace state
    val isFindReplaceVisible by viewModel.isFindReplaceVisible.collectAsState()
    val findText by viewModel.findText.collectAsState()
    val replaceText by viewModel.replaceText.collectAsState()
    val matchCase by viewModel.matchCase.collectAsState()
    val matchWholeWord by viewModel.matchWholeWord.collectAsState()
    val compileErrors by viewModel.compileErrors.collectAsState()
    val isCompiling by viewModel.isCompiling.collectAsState()
    val compileOutput by viewModel.compileOutput.collectAsState()
    val isTerminalVisible by viewModel.isTerminalVisible.collectAsState()
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    // Save As dialog state
    var showSaveAsDialog by remember { mutableStateOf(false) }
    
    // Directory picker launcher
    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            // Grant persistent permission
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            
            // Open the directory using the URI
            viewModel.openDirectoryFromUri(context, uri.toString())
        }
    }
    
    // Snackbar for status messages
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Show status messages
    LaunchedEffect(statusMessage) {
        statusMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.clearStatusMessage()
            }
        }
    }

    // Animate the width of the file explorer


    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        // No action needed on drag end
                    }
                ) { change, dragAmount ->
                    val threshold = 50f // Minimum drag distance to trigger action
                    
                    if (abs(dragAmount) > threshold) {
                        if (dragAmount > threshold) {
                            // Swiping right - show file explorer
                            if (!isFileExplorerVisible) {
                                viewModel.toggleFileExplorer()
                            }
                        } else if (dragAmount < -threshold) {
                            // Swiping left - hide file explorer
                            if (isFileExplorerVisible) {
                                viewModel.toggleFileExplorer()
                            }
                        }
                    }
                }
            }) {
            
            Row(modifier = Modifier
                .weight(1f)
                .fillMaxWidth()) {
                
                // File Explorer with animated width
                Surface(
                    modifier = Modifier

                        .clipToBounds()
                ) {
                    if (isFileExplorerVisible) {
                        FileExplorer(
                            files = files,
                            onFileClick = { fileName, filePath -> viewModel.openFile(fileName, filePath, context) },
                            onCreateFile = { fileName, parentPath -> viewModel.createNewFile(fileName, context, parentPath) },
                            onCreateDirectory = { dirName, parentPath -> viewModel.createNewDirectory(dirName, context, parentPath) },
                            onDeleteFile = { filePath -> viewModel.deleteFile(filePath, context) }, // Add this line
                            onOpenDirectory = { 
                                // Launch the system directory picker
                                directoryPickerLauncher.launch(null)
                            },
                            currentWorkspaceDir = currentWorkspaceDir,
                            modifier = Modifier.width(280.dp)
                        )
                    }
                }

                // Main Editor Area
                CodeEditor(
                    value = textFieldValue,
                    onValueChange = { viewModel.updateText(it) },
                    currentFile = currentFile,
                    currentFilePath = viewModel.currentFilePath.collectAsState().value,
                    isFileExplorerVisible = isFileExplorerVisible, // Add this line
                    onToggleFileExplorer = { viewModel.toggleFileExplorer() },
                    onNewFile = { viewModel.newFile() },
                    onSaveFile = { viewModel.saveCurrentFile(context) },
                    onSaveAsFile = { showSaveAsDialog = true },
                    onCopy = { viewModel.copy(context) },
                    onCut = { viewModel.cut(context) },
                    onPaste = { viewModel.paste(context) },
                    onUndo = { viewModel.undo() },
                    onRedo = { viewModel.redo() },
                    onShowFindReplace = { viewModel.showFindReplace() },
                    onCompile = { viewModel.compileCurrentFile() },
                    isCompiling = isCompiling,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 300.dp)
                )
            }
            
            Column(modifier = Modifier.fillMaxWidth()) {
                // Show terminal output panel at the bottom only when visible
                if (isTerminalVisible) {
                    TerminalOutputPanel(
                        output = terminalOutput,
                        isCompiling = isCompiling,
                        onDismiss = { viewModel.hideTerminal() }, // Re-enable hiding
                        onClearOutput = { viewModel.clearTerminalOutput() }
                    )
                }

                // Status Bar
                StatusBar(
                    wordCount = wordCount,
                    charCount = charCount,
                    currentFile = currentFile,
                    isDirty = isDirty
                )
            }
        }
        
        // Snackbar for status messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp)
        )
    }
    
    // Save As Dialog
    SaveAsDialog(
        isVisible = showSaveAsDialog,
        currentFileName = currentFile,
        onSaveAs = { fileName ->
            viewModel.saveAsFile(fileName, context)
            showSaveAsDialog = false
        },
        onDismiss = { showSaveAsDialog = false }
    )
    
    // Find and Replace Dialog
    FindReplaceDialog(
        isVisible = isFindReplaceVisible,
        findText = findText,
        replaceText = replaceText,
        matchCase = matchCase,
        matchWholeWord = matchWholeWord,
        onFindTextChange = { viewModel.updateFindText(it) },
        onReplaceTextChange = { viewModel.updateReplaceText(it) },
        onMatchCaseToggle = { viewModel.toggleMatchCase() },
        onMatchWholeWordToggle = { viewModel.toggleMatchWholeWord() },
        onFindNext = { viewModel.findNext() },
        onReplaceNext = { viewModel.replaceNext() },
        onReplaceAll = { viewModel.replaceAll() },
        onClose = { viewModel.hideFindReplace() }
    )
}