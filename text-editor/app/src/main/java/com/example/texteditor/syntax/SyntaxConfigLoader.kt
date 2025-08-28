package com.example.texteditor.syntax

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.IOException

class SyntaxConfigLoader(private val context: Context) {
    
    private val gson = Gson()

    fun loadConfiguration(): SyntaxConfiguration? {
        return try {
            val jsonString = context.assets.open("syntax_config.json").bufferedReader().use { it.readText() }
            val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)

            val version = jsonObject.get("version").asString
            val theme = parseTheme(jsonObject.getAsJsonObject("theme"))
            val languages = parseLanguages(jsonObject.getAsJsonArray("languages"))
            
            SyntaxConfiguration(version, theme, languages)
        } catch (e: IOException) {
            e.printStackTrace()
            getDefaultConfiguration()
        } catch (e: Exception) {
            e.printStackTrace()
            getDefaultConfiguration()
        }
    }
    
    private fun parseTheme(themeObject: JsonObject): SyntaxTheme {
        return SyntaxTheme(
            keywords = parseColor(themeObject.get("keywords").asString),
            strings = parseColor(themeObject.get("strings").asString),
            comments = parseColor(themeObject.get("comments").asString),
            numbers = parseColor(themeObject.get("numbers").asString),
            operators = parseColor(themeObject.get("operators").asString),
            functions = parseColor(themeObject.get("functions").asString),
            types = parseColor(themeObject.get("types").asString),
            default = parseColor(themeObject.get("default").asString)
        )
    }
    
    private fun parseLanguages(languagesArray: com.google.gson.JsonArray): List<LanguageRule> {
        return languagesArray.map { languageElement ->
            val languageObject = languageElement.asJsonObject
            LanguageRule(
                name = languageObject.get("name").asString,
                fileExtensions = languageObject.getAsJsonArray("fileExtensions").map { it.asString },
                keywords = languageObject.getAsJsonArray("keywords").map { it.asString },
                commentStart = languageObject.get("commentStart")?.asString,
                commentEnd = languageObject.get("commentEnd")?.asString,
                lineComment = languageObject.get("lineComment")?.asString,
                stringDelimiters = languageObject.getAsJsonArray("stringDelimiters").map { it.asString },
                numberPattern = languageObject.get("numberPattern").asString,
                operatorPattern = languageObject.get("operatorPattern").asString,
                functionPattern = languageObject.get("functionPattern").asString,
                typePattern = languageObject.get("typePattern").asString
            )
        }
    }
    
    private fun parseColor(colorString: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(colorString))
        } catch (e: IllegalArgumentException) {
            Color(0xFFD4D4D4) // Default color
        }
    }
    
    private fun getDefaultConfiguration(): SyntaxConfiguration {
        return SyntaxConfiguration(
            version = "1.0",
            theme = SyntaxTheme(),
            languages = listOf(
                LanguageRule(
                    name = "Plain Text",
                    fileExtensions = listOf(".txt"),
                    keywords = emptyList(),
                    commentStart = null,
                    commentEnd = null,
                    lineComment = null,
                    stringDelimiters = emptyList(),
                    numberPattern = "",
                    operatorPattern = "",
                    functionPattern = "",
                    typePattern = ""
                )
            )
        )
    }
}