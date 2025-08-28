package com.example.texteditor.compiler

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

// Update data classes to match Python server


class CompilerService {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val TAG = "CompilerService"
    
    // Update to Python server port
    private var serverUrl = "http://localhost:5000/compile"
    
    fun setServerUrl(url: String) {
        serverUrl = url
    }
    
    suspend fun compileCode(
        code: String, 
        fileName: String = "temp.kt",
        language: String = "kotlin"  // Add language parameter with default
    ): CompileResponse {
        return withContext(Dispatchers.IO) {
            try {
                val request = CompileRequest(code, language, fileName)
                val json = gson.toJson(request)
                
                Log.d(TAG, "Sending compile request to: $serverUrl")
                Log.d(TAG, "Request JSON: $json")
                
                val requestBody = json.toRequestBody("application/json".toMediaType())
                val httpRequest = Request.Builder()
                    .url(serverUrl)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = client.newCall(httpRequest).execute()
                val responseBody = response.body?.string()
                
                Log.d(TAG, "Response code: ${response.code}")
                Log.d(TAG, "Compile response: $responseBody")
                
                if (response.isSuccessful) {
                    if (responseBody != null) {
                        try {
                            val result = gson.fromJson(responseBody, CompileResponse::class.java)
                            Log.d(TAG, "Successfully parsed response: success=${result.success}, errors=${result.errors.size}")
                            result
                        } catch (e: JsonSyntaxException) {
                            Log.e(TAG, "JSON parsing error. Raw response: $responseBody", e)
                            CompileResponse(
                                success = false,
                                output = "JSON parsing error: ${e.message}",
                                errors = listOf("Server returned invalid JSON: $responseBody")
                            )
                        }
                    } else {
                        CompileResponse(false, "Empty response from server", listOf("No response body"))
                    }
                } else {
                    Log.e(TAG, "HTTP error: ${response.code}, body: $responseBody")
                    CompileResponse(
                        success = false,
                        output = "HTTP Error: ${response.code}",
                        errors = listOf("Server error: ${response.code} - ${responseBody ?: "No error details"}")
                    )
                }
            } catch (e: IOException) {
                Log.e(TAG, "Network error", e)
                CompileResponse(
                    success = false,
                    output = "Network error: ${e.message}",
                    errors = listOf("Cannot connect to compiler server. Make sure the server is running on port 5000.")
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error", e)
                CompileResponse(
                    success = false,
                    output = "Unexpected error: ${e.message}",
                    errors = listOf("Internal error: ${e.javaClass.simpleName} - ${e.message}")
                )
            }
        }
    }
    
    suspend fun testConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val healthUrl = serverUrl.replace("/compile", "/health")
                val request = Request.Builder()
                    .url(healthUrl)
                    .get()
                    .build()
                
                Log.d(TAG, "Testing connection to: $healthUrl")
                val response = client.newCall(request).execute()
                val isSuccessful = response.isSuccessful
                Log.d(TAG, "Connection test result: $isSuccessful")
                isSuccessful
            } catch (e: Exception) {
                Log.e(TAG, "Connection test failed", e)
                false
            }
        }
    }
    
    suspend fun getSupportedLanguages(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val languagesUrl = serverUrl.replace("/compile", "/languages")
                val request = Request.Builder()
                    .url(languagesUrl)
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val languagesResponse = gson.fromJson(responseBody, Map::class.java)
                    @Suppress("UNCHECKED_CAST")
                    (languagesResponse["languages"] as? List<String>) ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get supported languages", e)
                emptyList()
            }
        }
    }
}