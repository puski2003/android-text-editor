package com.example.texteditor.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.texteditor.model.FileItem
import java.io.File
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.os.Environment
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.texteditor.compiler.CompilerError
import com.example.texteditor.compiler.CompilerService
import kotlin.text.Regex
import android.webkit.MimeTypeMap

class TextEditorViewModel : ViewModel() {

    private val _textFieldValue = MutableStateFlow(TextFieldValue(""))
    val textFieldValue: StateFlow<TextFieldValue> = _textFieldValue.asStateFlow()

    private val _isDirty = MutableStateFlow(false)
    val isDirty: StateFlow<Boolean> = _isDirty.asStateFlow()

    private val _currentFile = MutableStateFlow<String?>(null)
    val currentFile: StateFlow<String?> = _currentFile.asStateFlow()

    private val _currentFilePath = MutableStateFlow<String?>(null)
    val currentFilePath: StateFlow<String?> = _currentFilePath.asStateFlow()

    private val _currentFileUri = MutableStateFlow<String?>(null)
    val currentFileUri: StateFlow<String?> = _currentFileUri.asStateFlow()

    private val _currentWorkspaceDir = MutableStateFlow<String?>(null)
    val currentWorkspaceDir: StateFlow<String?> = _currentWorkspaceDir.asStateFlow()

    private val _currentWorkspaceUri = MutableStateFlow<String?>(null)
    val currentWorkspaceUri: StateFlow<String?> = _currentWorkspaceUri.asStateFlow()

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _isFileExplorerVisible = MutableStateFlow(true)
    val isFileExplorerVisible: StateFlow<Boolean> = _isFileExplorerVisible.asStateFlow()

    // Auto-save state
    private val _isAutoSaveEnabled = MutableStateFlow(true)
    val isAutoSaveEnabled: StateFlow<Boolean> = _isAutoSaveEnabled.asStateFlow()

    // Undo/Redo functionality
    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()
    private var maxUndoHistory = 50

    // Find and Replace state
    private val _findText = MutableStateFlow("")
    val findText: StateFlow<String> = _findText.asStateFlow()

    private val _replaceText = MutableStateFlow("")
    val replaceText: StateFlow<String> = _replaceText.asStateFlow()

    private val _isFindReplaceVisible = MutableStateFlow(false)
    val isFindReplaceVisible: StateFlow<Boolean> = _isFindReplaceVisible.asStateFlow()

    private val _matchCase = MutableStateFlow(false)
    val matchCase: StateFlow<Boolean> = _matchCase.asStateFlow()

    private val _matchWholeWord = MutableStateFlow(false)
    val matchWholeWord: StateFlow<Boolean> = _matchWholeWord.asStateFlow()

    // Statistics
    private val _wordCount = MutableStateFlow(0)
    val wordCount: StateFlow<Int> = _wordCount.asStateFlow()

    private val _charCount = MutableStateFlow(0)
    val charCount: StateFlow<Int> = _charCount.asStateFlow()

    // Error/status messages
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Add these new properties
    private val compilerService = CompilerService()

    private val _compileErrors = MutableStateFlow<List<CompilerError>>(emptyList())
    val compileErrors: StateFlow<List<CompilerError>> = _compileErrors.asStateFlow()

    private val _isCompiling = MutableStateFlow(false)
    val isCompiling: StateFlow<Boolean> = _isCompiling.asStateFlow()

    private val _compileOutput = MutableStateFlow<String?>(null)
    val compileOutput: StateFlow<String?> = _compileOutput.asStateFlow()

    // Change terminal visibility back to false initially
    private val _isTerminalVisible = MutableStateFlow(false)  // Changed back to false
    val isTerminalVisible: StateFlow<Boolean> = _isTerminalVisible.asStateFlow()

    private val _terminalOutput = MutableStateFlow<String?>(null)
    val terminalOutput: StateFlow<String?> = _terminalOutput.asStateFlow()

    init {
        // Initialize with default workspace
        initializeDefaultWorkspace()

        // Auto-save functionality
        viewModelScope.launch {
            while (true) {
                delay(30000) // Auto-save every 30 seconds
                if (_isDirty.value && _isAutoSaveEnabled.value && _currentFileUri.value != null) {
                    saveCurrentFile()
                }
            }
        }
    }

    private fun initializeDefaultWorkspace() {
        try {
            // Try to use Documents directory, fallback to app-specific directory
            val documentsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "TextEditor"
            )

            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }

            if (documentsDir.exists() && documentsDir.canWrite()) {
                openDirectory(documentsDir.absolutePath)
            } else {
                _statusMessage.value = "Please select a directory to get started"
            }
        } catch (e: Exception) {
            _statusMessage.value = "Error initializing workspace: ${e.message}"
        }
    }

    fun openDirectoryFromUri(context: Context, uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            val documentFile = DocumentFile.fromTreeUri(context, uri)

            if (documentFile != null && documentFile.isDirectory) {
                _currentWorkspaceUri.value = uriString
                _currentWorkspaceDir.value = documentFile.name ?: "Selected Directory"
                loadDirectoryFromUri(context, documentFile)

            } else {
                _statusMessage.value = "Invalid directory selected"
            }
        } catch (e: Exception) {
            _statusMessage.value = "Error opening directory: ${e.message}"
        }
    }

    private fun loadDirectoryFromUri(context: Context, documentFile: DocumentFile) {
        try {
            val fileItems = mutableListOf<FileItem>()

            documentFile.listFiles().sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy {
                it.name?.lowercase() ?: ""
            }).forEach { file ->
                val fileName = file.name ?: "Unknown"
                if (file.isDirectory) {
                    val children = loadDirectoryChildrenFromUri(file)
                    fileItems.add(FileItem(fileName, file.uri.toString(), true, children))
                } else {
                    fileItems.add(FileItem(fileName, file.uri.toString(), false))
                }
            }

            _files.value = fileItems
        } catch (e: Exception) {
            _statusMessage.value = "Error loading directory files: ${e.message}"
        }
    }

    private fun loadDirectoryChildrenFromUri(documentFile: DocumentFile): List<FileItem> {
        return try {
            val children = mutableListOf<FileItem>()
            documentFile.listFiles().sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy {
                it.name?.lowercase() ?: ""
            }).forEach { file ->
                val fileName = file.name ?: "Unknown"
                if (file.isDirectory) {
                    val subChildren = loadDirectoryChildrenFromUri(file)
                    children.add(FileItem(fileName, file.uri.toString(), true, subChildren))
                } else {
                    children.add(FileItem(fileName, file.uri.toString(), false))
                }
            }
            children
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun openDirectory(directoryPath: String) {
        try {
            val directory = File(directoryPath)
            if (directory.exists() && directory.isDirectory) {
                _currentWorkspaceDir.value = directoryPath
                _currentWorkspaceUri.value = null // Clear URI when using file path
                loadDirectoryFiles(directory)

            } else {
                _statusMessage.value = "Invalid directory path"
            }
        } catch (e: Exception) {
            _statusMessage.value = "Error opening directory: ${e.message}"
        }
    }

    private fun loadDirectoryFiles(directory: File) {
        try {
            val fileItems = mutableListOf<FileItem>()

            directory.listFiles()
                ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
                ?.forEach { file ->
                    if (file.isDirectory) {
                        val children = loadDirectoryChildren(file)
                        fileItems.add(FileItem(file.name, file.absolutePath, true, children))
                    } else {
                        fileItems.add(FileItem(file.name, file.absolutePath, false))
                    }
                }

            _files.value = fileItems
        } catch (e: Exception) {
            _statusMessage.value = "Error loading directory files: ${e.message}"
        }
    }

    private fun loadDirectoryChildren(directory: File): List<FileItem> {
        return try {
            val children = mutableListOf<FileItem>()
            directory.listFiles()
                ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
                ?.forEach { file ->
                    if (file.isDirectory) {
                        val subChildren = loadDirectoryChildren(file)
                        children.add(FileItem(file.name, file.absolutePath, true, subChildren))
                    } else {
                        children.add(FileItem(file.name, file.absolutePath, false))
                    }
                }
            children
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun refreshFileExplorer() {
        _currentWorkspaceDir.value?.let { workspaceDir ->
            loadDirectoryFiles(File(workspaceDir))
        }
    }

    fun createNewFile(fileName: String, context: Context? = null, parentPath: String? = null) {
        viewModelScope.launch {
            try {
                val workspaceUri = _currentWorkspaceUri.value
                val workspaceDir = _currentWorkspaceDir.value

                if (workspaceUri != null && context != null) {
                    // Create file using DocumentFile API
                    val uri = Uri.parse(workspaceUri)
                    val documentFile = if (parentPath != null) {
                        // Create in specific folder
                        val parentUri = Uri.parse(parentPath)
                        DocumentFile.fromTreeUri(context, parentUri)
                    } else {
                        // Create in root workspace
                        DocumentFile.fromTreeUri(context, uri)
                    }

                    if (documentFile != null) {
                        val mimeType = getMimeTypeFromFileName(fileName)
                        val newFile = documentFile.createFile(mimeType, fileName)
                        if (newFile != null) {
                            refreshFileExplorerFromUri(context)
                            openFileFromUri(context, newFile.uri.toString())
                        } else {
                            _statusMessage.value = "Failed to create file: $fileName"
                        }
                    }
                } else if (workspaceDir != null) {
                    // Create file using regular File API
                    val targetDir = if (parentPath != null) {
                        File(parentPath)
                    } else {
                        File(workspaceDir)
                    }

                    val newFile = File(targetDir, fileName)
                    if (!newFile.exists()) {
                        newFile.createNewFile()
                        refreshFileExplorer()
                        openFileFromPath(newFile.absolutePath)
                    } else {
                        _statusMessage.value = "File already exists: $fileName"
                    }
                } else {
                    _statusMessage.value = "No workspace directory selected"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Error creating file: ${e.message}"
            }
        }
    }

    fun createNewDirectory(dirName: String, context: Context? = null, parentPath: String? = null) {
        try {
            val workspaceUri = _currentWorkspaceUri.value
            val workspaceDir = _currentWorkspaceDir.value

            if (workspaceUri != null && context != null) {
                // Create directory using DocumentFile API
                val uri = Uri.parse(workspaceUri)
                val documentFile = if (parentPath != null) {
                    // Create in specific folder
                    val parentUri = Uri.parse(parentPath)
                    DocumentFile.fromTreeUri(context, parentUri)
                } else {
                    // Create in root workspace
                    DocumentFile.fromTreeUri(context, uri)
                }

                if (documentFile != null) {
                    val newDir = documentFile.createDirectory(dirName)
                    if (newDir != null) {
                        refreshFileExplorerFromUri(context)
                        _statusMessage.value = "Created directory: $dirName"
                    } else {
                        _statusMessage.value = "Failed to create directory: $dirName"
                    }
                }
            } else if (workspaceDir != null) {
                // Create directory using regular File API
                val targetDir = if (parentPath != null) {
                    File(parentPath)
                } else {
                    File(workspaceDir)
                }

                val newDir = File(targetDir, dirName)
                if (!newDir.exists()) {
                    newDir.mkdirs()
                    refreshFileExplorer()
                    _statusMessage.value = "Created directory: $dirName"
                } else {
                    _statusMessage.value = "Directory already exists: $dirName"
                }
            } else {
                _statusMessage.value = "No workspace directory selected"
            }
        } catch (e: Exception) {
            _statusMessage.value = "Error creating directory: ${e.message}"
        }
    }

    private fun refreshFileExplorerFromUri(context: Context) {
        _currentWorkspaceUri.value?.let { uriString ->
            val uri = Uri.parse(uriString)
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            if (documentFile != null) {
                loadDirectoryFromUri(context, documentFile)
            }
        }
    }

    fun openFile(fileName: String, filePath: String, context: Context? = null) {
        if (filePath.startsWith("content://")) {
            // Handle URI-based files
            if (context != null) {
                openFileFromUri(context, filePath)
            }
        } else {
            // Handle regular file paths
            openFileFromPath(filePath)
        }
    }

    private fun openFileFromUri(context: Context, uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            val documentFile = DocumentFile.fromSingleUri(context, uri)

            if (documentFile != null && documentFile.exists()) {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""

                _currentFile.value = documentFile.name
                _currentFileUri.value = uriString
                _currentFilePath.value = null // Clear file path when using URI
                _textFieldValue.value = TextFieldValue(content)
                _isDirty.value = false
                undoStack.clear()
                redoStack.clear()
                updateStatistics(content)

            } else {
                _statusMessage.value = "File not found or inaccessible"
            }
        } catch (e: Exception) {
            _statusMessage.value = "Error opening file: ${e.message}"
        }
    }

    fun openFileFromPath(filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists() && file.isFile) {
                val content = file.readText()
                _currentFile.value = file.name
                _currentFilePath.value = filePath
                _currentFileUri.value = null // Clear URI when using file path
                _textFieldValue.value = TextFieldValue(content)
                _isDirty.value = false
                undoStack.clear()
                redoStack.clear()
                updateStatistics(content)

            } else {
                _statusMessage.value = "File not found: $filePath"
            }
        } catch (e: Exception) {
            _statusMessage.value = "Error opening file: ${e.message}"
        }
    }

    fun saveCurrentFile(context: Context? = null) {
        val fileUri = _currentFileUri.value
        val filePath = _currentFilePath.value

        if (fileUri != null && context != null) {
            saveToUri(context, fileUri)
        } else if (filePath != null) {
            saveToFile(filePath)
        } else {
            // Create new file in workspace
            val fileName = _currentFile.value ?: "untitled.txt"
            if (context != null && _currentWorkspaceUri.value != null) {
                createNewFile(fileName, context)
            } else if (_currentWorkspaceDir.value != null) {
                val newFilePath = File(_currentWorkspaceDir.value!!, fileName).absolutePath
                _currentFilePath.value = newFilePath
                saveToFile(newFilePath)
            } else {
                _statusMessage.value = "No workspace directory selected for saving"
            }
        }
    }

    private fun saveToUri(context: Context, uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            val outputStream = context.contentResolver.openOutputStream(uri)
            outputStream?.use { stream ->
                stream.write(_textFieldValue.value.text.toByteArray())
            }

            _isDirty.value = false
            val documentFile = DocumentFile.fromSingleUri(context, uri)
            _currentFile.value = documentFile?.name
            _statusMessage.value = "File saved: ${documentFile?.name}"
        } catch (e: Exception) {
            _statusMessage.value = "Error saving file: ${e.message}"
        }
    }

    private fun saveToFile(filePath: String) {
        try {
            val file = File(filePath)
            file.writeText(_textFieldValue.value.text)
            _isDirty.value = false
            _currentFilePath.value = filePath
            _currentFile.value = file.name
            refreshFileExplorer()
            _statusMessage.value = "File saved: ${file.name}"
        } catch (e: Exception) {
            _statusMessage.value = "Error saving file: ${e.message}"
        }
    }

    fun updateText(newValue: TextFieldValue) {
        // Save current state for undo
        if (_textFieldValue.value.text != newValue.text) {
            undoStack.add(_textFieldValue.value)
            if (undoStack.size > maxUndoHistory) {
                undoStack.removeAt(0)
            }
            redoStack.clear()
        }

        _textFieldValue.value = newValue
        updateStatistics(newValue.text)

        if (!_isDirty.value) {
            _isDirty.value = true
        }
    }

    private fun updateStatistics(text: String) {
        _charCount.value = text.length
        _wordCount.value = if (text.isBlank()) 0 else text.split("\\s+".toRegex()).size
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(_textFieldValue.value)
            val previousState = undoStack.removeAt(undoStack.size - 1)
            _textFieldValue.value = previousState
            updateStatistics(previousState.text)
            _isDirty.value = true
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(_textFieldValue.value)
            val nextState = redoStack.removeAt(redoStack.size - 1)
            _textFieldValue.value = nextState
            updateStatistics(nextState.text)
            _isDirty.value = true
        }
    }

    fun copy(context: Context) {
        val selection = _textFieldValue.value.selection
        if (!selection.collapsed) {
            val selectedText = _textFieldValue.value.text.substring(selection.start, selection.end)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("text", selectedText)
            clipboard.setPrimaryClip(clip)
            _statusMessage.value = "Text copied to clipboard"
        }
    }

    fun cut(context: Context) {
        val selection = _textFieldValue.value.selection
        if (!selection.collapsed) {
            copy(context)
            val newText = _textFieldValue.value.text.removeRange(selection.start, selection.end)
            updateText(
                _textFieldValue.value.copy(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(selection.start)
                )
            )
            _statusMessage.value = "Text cut to clipboard"
        }
    }

    fun paste(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val pastedText = clipData.getItemAt(0).text.toString()
            val selection = _textFieldValue.value.selection
            val newText =
                _textFieldValue.value.text.replaceRange(selection.start, selection.end, pastedText)
            val newCursorPosition = selection.start + pastedText.length
            updateText(
                _textFieldValue.value.copy(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(newCursorPosition)
                )
            )
            _statusMessage.value = "Text pasted from clipboard"
        }
    }

    fun newFile() {
        if (_isDirty.value && (_currentFilePath.value != null || _currentFileUri.value != null)) {
            // In a real app, you'd show a dialog asking to save
        }
        _currentFile.value = "Untitled"
        _currentFilePath.value = null
        _currentFileUri.value = null
        _textFieldValue.value = TextFieldValue("")
        _isDirty.value = false
        undoStack.clear()
        redoStack.clear()
        updateStatistics("")
    }

    // Find and Replace functionality
    fun showFindReplace() {
        _isFindReplaceVisible.value = true
    }

    fun hideFindReplace() {
        _isFindReplaceVisible.value = false
    }

    fun updateFindText(text: String) {
        _findText.value = text
    }

    fun updateReplaceText(text: String) {
        _replaceText.value = text
    }

    fun toggleMatchCase() {
        _matchCase.value = !_matchCase.value
    }

    fun toggleMatchWholeWord() {
        _matchWholeWord.value = !_matchWholeWord.value
    }

    fun findNext() {
        val searchText = _findText.value
        if (searchText.isNotEmpty()) {
            val text = _textFieldValue.value.text
            val currentPosition = _textFieldValue.value.selection.end

            val regex = if (_matchCase.value) {
                if (_matchWholeWord.value) {
                    Regex("\\b${Regex.escape(searchText)}\\b")
                } else {
                    Regex(Regex.escape(searchText))
                }
            } else {
                if (_matchWholeWord.value) {
                    Regex("\\b${Regex.escape(searchText)}\\b", RegexOption.IGNORE_CASE)
                } else {
                    Regex(Regex.escape(searchText), RegexOption.IGNORE_CASE)
                }
            }

            val match = regex.find(text, currentPosition) ?: regex.find(text, 0)

            match?.let {
                val newSelection =
                    androidx.compose.ui.text.TextRange(it.range.first, it.range.last + 1)
                _textFieldValue.value = _textFieldValue.value.copy(selection = newSelection)
            }
        }
    }

    fun replaceNext() {
        val searchText = _findText.value
        val replaceText = _replaceText.value

        if (searchText.isNotEmpty()) {
            val selection = _textFieldValue.value.selection
            if (!selection.collapsed) {
                val selectedText =
                    _textFieldValue.value.text.substring(selection.start, selection.end)

                val matches = if (_matchWholeWord.value) {
                    val regex = if (_matchCase.value) {
                        Regex("\\b${Regex.escape(searchText)}\\b")
                    } else {
                        Regex("\\b${Regex.escape(searchText)}\\b", RegexOption.IGNORE_CASE)
                    }
                    regex.matches(selectedText)
                } else {
                    if (_matchCase.value) {
                        selectedText == searchText
                    } else {
                        selectedText.equals(searchText, ignoreCase = true)
                    }
                }

                if (matches) {
                    val newText = _textFieldValue.value.text.replaceRange(
                        selection.start,
                        selection.end,
                        replaceText
                    )
                    val newCursorPosition = selection.start + replaceText.length
                    updateText(
                        _textFieldValue.value.copy(
                            text = newText,
                            selection = androidx.compose.ui.text.TextRange(newCursorPosition)
                        )
                    )
                }
            }
            findNext()
        }
    }

    fun replaceAll() {
        val searchText = _findText.value
        val replaceText = _replaceText.value

        if (searchText.isNotEmpty()) {
            val text = _textFieldValue.value.text

            val regex = if (_matchCase.value) {
                if (_matchWholeWord.value) {
                    Regex("\\b${Regex.escape(searchText)}\\b")
                } else {
                    Regex(Regex.escape(searchText))
                }
            } else {
                if (_matchWholeWord.value) {
                    Regex("\\b${Regex.escape(searchText)}\\b", RegexOption.IGNORE_CASE)
                } else {
                    Regex(Regex.escape(searchText), RegexOption.IGNORE_CASE)
                }
            }

            val newText = regex.replace(text, replaceText)

            if (newText != text) {
                updateText(_textFieldValue.value.copy(text = newText))
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun toggleFileExplorer() {
        _isFileExplorerVisible.value = !_isFileExplorerVisible.value
    }

    fun toggleAutoSave() {
        _isAutoSaveEnabled.value = !_isAutoSaveEnabled.value
    }

    fun saveAsFile(fileName: String, context: Context? = null) {
        try {
            val workspaceUri = _currentWorkspaceUri.value
            val workspaceDir = _currentWorkspaceDir.value

            if (workspaceUri != null && context != null) {
                // Save using DocumentFile API with proper MIME type
                val uri = Uri.parse(workspaceUri)
                val documentFile = DocumentFile.fromTreeUri(context, uri)

                if (documentFile != null) {
                    val mimeType = getMimeTypeFromFileName(fileName)
                    val newFile = documentFile.createFile(mimeType, fileName)
                    if (newFile != null) {
                        val outputStream = context.contentResolver.openOutputStream(newFile.uri)
                        outputStream?.use { stream ->
                            stream.write(_textFieldValue.value.text.toByteArray())
                        }

                        _currentFileUri.value = newFile.uri.toString()
                        _currentFilePath.value = null
                        _currentFile.value = fileName
                        _isDirty.value = false
                        refreshFileExplorerFromUri(context)
                        _statusMessage.value = "File saved as: $fileName"
                    } else {
                        _statusMessage.value = "Failed to create file: $fileName"
                    }
                }
            } else if (workspaceDir != null) {
                // Save using regular File API
                val newFilePath = File(workspaceDir, fileName).absolutePath
                val file = File(newFilePath)
                file.writeText(_textFieldValue.value.text)

                _currentFilePath.value = newFilePath
                _currentFileUri.value = null
                _currentFile.value = fileName
                _isDirty.value = false
                refreshFileExplorer()
                _statusMessage.value = "File saved as: $fileName"
            } else {
                _statusMessage.value = "No workspace directory selected"
            }
        } catch (e: Exception) {
            _statusMessage.value = "Error saving file as: ${e.message}"
        }
    }

    // Update the compileCurrentFile function
    fun compileCurrentFile() {
        val currentFileName = _currentFile.value
        val language = getLanguageFromFile(currentFileName)

        viewModelScope.launch {
            // Clear terminal and show compiling status immediately
            _terminalOutput.value = null
            _isCompiling.value = true
            _isTerminalVisible.value = true // Show terminal when compilation starts
            _statusMessage.value = "Compiling $language code..."

            // Add a small delay to show the "Compiling..." message
            delay(100)

            try {
                val response = compilerService.compileCode(
                    code = _textFieldValue.value.text,
                    fileName = currentFileName ?: "temp.${getDefaultExtension(language)}",
                    language = language
                )

                // Convert List<String> to List<CompilerError> for internal use
                val compilerErrors = response.errors.map { errorMessage ->
                    CompilerError(
                        message = errorMessage,
                        line = null,
                        column = null,
                        severity = "ERROR"
                    )
                }
                _compileErrors.value = compilerErrors

                // Create terminal output that includes errors
                val terminalOutput = if (response.success) {
                    if (response.errors.isNotEmpty()) {
                        // Success but with warnings
                        "${response.output}\n\n--- Warnings ---\n${response.errors.joinToString("\n")}"
                    } else {
                        response.output
                    }
                } else {
                    // Compilation failed
                    val errorSection = if (response.errors.isNotEmpty()) {
                        "\n--- Compilation Errors ---\n${response.errors.joinToString("\n")}"
                    } else {
                        ""
                    }
                    "${response.output}$errorSection"
                }

                _terminalOutput.value = terminalOutput

                if (response.success) {
                    _statusMessage.value = if (response.errors.isNotEmpty()) {
                        "$language compilation completed with ${response.errors.size} warnings"
                    } else {
                        "$language compilation and execution completed successfully"
                    }
                } else {
                    _statusMessage.value =
                        "$language compilation failed: ${response.errors.size} errors"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Compilation error: ${e.message}"
                _terminalOutput.value = "--- Compilation Error ---\nError: ${e.message}"
                _compileErrors.value = emptyList()
            } finally {
                _isCompiling.value = false
            }
        }
    }

    private fun getLanguageFromFile(fileName: String?): String {
        if (fileName == null) return "python" // Default to Python

        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "kt" -> "kotlin"
            "py" -> "python"
            "java" -> "java"
            "c" -> "c"
            "cpp", "cc", "cxx" -> "cpp"
            "js" -> "javascript"
            "go" -> "go"
            else -> "python" // Default fallback
        }
    }

    private fun getDefaultExtension(language: String): String {
        return when (language) {
            "kotlin" -> "kt"
            "python" -> "py"
            "java" -> "java"
            "c" -> "c"
            "cpp" -> "cpp"
            "javascript" -> "js"
            "go" -> "go"
            else -> "py"
        }
    }

    fun showTerminal() {
        _isTerminalVisible.value = true
    }

    fun hideTerminal() {
        _isTerminalVisible.value = false
    }


    // Keep this function for clearing output
    fun clearTerminalOutput() {
        _terminalOutput.value = null
    }

    private fun getMimeTypeFromFileName(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "text/plain"
    }

// Replace the deleteFile function (around lines 860-920) with this corrected version:

    fun deleteFile(filePath: String, context: Context? = null) {
        viewModelScope.launch {
            try {
                if (filePath.startsWith("content://")) {
                    // Handle URI-based files (DocumentFile API)
                    if (context != null) {
                        val uri = Uri.parse(filePath)
                        val documentFile = DocumentFile.fromSingleUri(context, uri)
                        if (documentFile != null && documentFile.exists()) {
                            val fileName = documentFile.name ?: "Unknown"
                            val isDirectory = documentFile.isDirectory

                            val deleted = documentFile.delete()
                            if (deleted) {
                                // If the deleted file is currently open, close it
                                if (_currentFileUri.value == filePath) {
                                    newFile() // Clear the editor
                                }
                                refreshFileExplorerFromUri(context)
                                _statusMessage.value = "Deleted ${if (isDirectory) "directory" else "file"}: $fileName"
                            } else {
                                _statusMessage.value = "Failed to delete: $fileName (may be protected or in use)"
                            }
                        } else {
                            _statusMessage.value = "File not found or inaccessible"
                        }
                    } else {
                        _statusMessage.value = "Context required for URI-based file deletion"
                    }
                } else {
                    // Handle regular file paths
                    val file = File(filePath)
                    if (file.exists()) {
                        val fileName = file.name
                        val isDirectory = file.isDirectory

                        // Check if file is currently open
                        val isCurrentlyOpen = _currentFilePath.value == filePath

                        val deleted = if (isDirectory) {
                            // Count items in directory before deletion
                            val itemCount = file.listFiles()?.size ?: 0
                            if (itemCount > 0) {
                                // Directory is not empty, ask for confirmation (this would need UI state)
                                _statusMessage.value = "Directory contains $itemCount items. Delete anyway..."
                                delay(1000) // Give user time to see the message
                            }
                            file.deleteRecursively()
                        } else {
                            file.delete()
                        }

                        if (deleted) {
                            // If the deleted file is currently open, close it
                            if (isCurrentlyOpen) {
                                newFile() // Clear the editor
                            }
                            refreshFileExplorer()
                            _statusMessage.value = "Successfully deleted ${if (isDirectory) "directory" else "file"}: $fileName"
                        } else {
                            _statusMessage.value = "Failed to delete: $fileName (may be protected, in use, or permission denied)"
                        }
                    } else {
                        _statusMessage.value = "File not found: ${File(filePath).name}"
                    }
                }
            } catch ( e: SecurityException) {
                _statusMessage.value = "Permission denied: Cannot delete file"
            } catch ( e: Exception) {
                _statusMessage.value = "Error deleting file: ${e.message}"
            }
        }
    }

    // Add this function to your TextEditorViewModel class
}