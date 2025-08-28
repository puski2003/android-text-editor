package com.example.texteditor.compiler

import com.google.gson.annotations.SerializedName

data class CompileRequest(
    val code: String,
    val language: String,  // Add language parameter
    val fileName: String? = null
)

data class CompileResponse(
    val success: Boolean,
    val output: String,
    val errors: List<String> = emptyList()
)
data class CompilerError(
    val message: String,
    val line: Int? = null,        // Make nullable with default null
    val column: Int? = null,      // Make nullable with default null
    val severity: String = "ERROR"
)

enum class ErrorSeverity {
    @SerializedName("ERROR")
    ERROR,
    @SerializedName("WARNING")
    WARNING,
    @SerializedName("INFO")
    INFO
}