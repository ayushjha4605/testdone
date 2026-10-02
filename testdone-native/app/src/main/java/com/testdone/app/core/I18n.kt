package com.testdone.app.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * App translations — same string table as the web app (6 languages),
 * loaded once from assets/content/i18n.json.
 */
class I18n(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val table = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    private var loaded = false

    val languages: List<Triple<String, String, String>> = listOf(
        Triple("en", "English", "English"),
        Triple("hi", "Hindi", "हिंदी"),
        Triple("ta", "Tamil", "தமிழ்"),
        Triple("te", "Telugu", "తెలుగు"),
        Triple("bn", "Bengali", "বাংলা"),
        Triple("mr", "Marathi", "मराठी"),
    )

    private val _current = MutableStateFlow("en")
    val current: StateFlow<String> = _current

    suspend fun load() {
        if (loaded) return
        withContext(Dispatchers.IO) {
            runCatching {
                val text = context.assets.open("content/i18n.json").bufferedReader().use { it.readText() }
                val root = json.parseToJsonElement(text).jsonObject
                table.value = root.entries.associate { (lang, obj) ->
                    lang to obj.jsonObject.entries.associate { (k, v) -> k to (v.jsonPrimitive.contentOrNull ?: "") }
                }
                loaded = true
            }
        }
    }

    fun setLanguage(code: String) { _current.value = code }

    /** Translate a key in the current language, falling back to English. */
    fun t(key: String): String {
        val lang = _current.value
        return table.value[lang]?.get(key)
            ?: table.value["en"]?.get(key)
            ?: key
    }
}
