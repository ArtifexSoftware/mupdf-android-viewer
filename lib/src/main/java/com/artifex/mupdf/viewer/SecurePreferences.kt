package com.artifex.mupdf.viewer

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePreferences {
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_OPENAI_API_KEY = "openai_api_key"

    private fun getEncryptedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(context: Context, apiKey: String) {
        getEncryptedPrefs(context).edit().putString(KEY_OPENAI_API_KEY, apiKey).apply()
    }

    fun getApiKey(context: Context): String? {
        return getEncryptedPrefs(context).getString(KEY_OPENAI_API_KEY, null)
    }

    fun hasApiKey(context: Context): Boolean {
        return !getApiKey(context).isNullOrEmpty()
    }
}
