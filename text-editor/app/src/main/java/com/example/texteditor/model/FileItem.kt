package com.example.texteditor.model

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean = false,
    val children: List<FileItem> = emptyList()
)