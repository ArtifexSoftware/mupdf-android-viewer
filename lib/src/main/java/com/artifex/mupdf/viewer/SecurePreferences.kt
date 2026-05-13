package com.artifex.mupdf.viewer

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePreferences {
    private const val PREFS_NAME = "secure_prefs"
    
    // API Keys for different providers
    private const val KEY_OPENAI_API_KEY = "openai_api_key"
    private const val KEY_DEEPSEEK_API_KEY = "deepseek_api_key"
    private const val KEY_QWEN_API_KEY = "qwen_api_key"
    private const val KEY_ANTHROPIC_API_KEY = "anthropic_api_key"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    
    private const val KEY_AI_MODEL = "ai_model"
    private const val KEY_AI_BASE_URL = "ai_base_url"

    private const val DEFAULT_MODEL = "gpt-4o-mini"
    private const val DEFAULT_BASE_URL = "https://api.openai.com/v1"

    private fun getEncryptedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Specific save/get for each provider
    fun saveOpenAiKey(context: Context, key: String) = getEncryptedPrefs(context).edit().putString(KEY_OPENAI_API_KEY, key).apply()
    fun getOpenAiKey(context: Context) = getEncryptedPrefs(context).getString(KEY_OPENAI_API_KEY, "")

    fun saveDeepSeekKey(context: Context, key: String) = getEncryptedPrefs(context).edit().putString(KEY_DEEPSEEK_API_KEY, key).apply()
    fun getDeepSeekKey(context: Context) = getEncryptedPrefs(context).getString(KEY_DEEPSEEK_API_KEY, "")

    fun saveQwenKey(context: Context, key: String) = getEncryptedPrefs(context).edit().putString(KEY_QWEN_API_KEY, key).apply()
    fun getQwenKey(context: Context) = getEncryptedPrefs(context).getString(KEY_QWEN_API_KEY, "")

    fun saveAnthropicKey(context: Context, key: String) = getEncryptedPrefs(context).edit().putString(KEY_ANTHROPIC_API_KEY, key).apply()
    fun getAnthropicKey(context: Context) = getEncryptedPrefs(context).getString(KEY_ANTHROPIC_API_KEY, "")

    fun saveGeminiKey(context: Context, key: String) = getEncryptedPrefs(context).edit().putString(KEY_GEMINI_API_KEY, key).apply()
    fun getGeminiKey(context: Context) = getEncryptedPrefs(context).getString(KEY_GEMINI_API_KEY, "")

    // Legacy method for backward compatibility or simple usage
    fun getApiKey(context: Context): String? {
        val model = getModel(context).lowercase()
        val url = getBaseUrl(context).lowercase()
        return when {
            url.contains("deepseek") || model.contains("deepseek") -> getDeepSeekKey(context)
            url.contains("dashscope") || url.contains("qwen") || model.contains("qwen") -> getQwenKey(context)
            url.contains("anthropic") || model.contains("claude") -> getAnthropicKey(context)
            url.contains("googleapis") || model.contains("gemini") -> getGeminiKey(context)
            else -> getOpenAiKey(context)
        }
    }

    fun saveModel(context: Context, model: String) {
        getEncryptedPrefs(context).edit().putString(KEY_AI_MODEL, model).apply()
    }

    fun getModel(context: Context): String {
        return getEncryptedPrefs(context).getString(KEY_AI_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    fun saveBaseUrl(context: Context, baseUrl: String) {
        getEncryptedPrefs(context).edit().putString(KEY_AI_BASE_URL, baseUrl).apply()
    }

    fun getBaseUrl(context: Context): String {
        return getEncryptedPrefs(context).getString(KEY_AI_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun hasApiKey(context: Context): Boolean {
        val key = getApiKey(context)
        return !key.isNullOrEmpty()
    }
}
