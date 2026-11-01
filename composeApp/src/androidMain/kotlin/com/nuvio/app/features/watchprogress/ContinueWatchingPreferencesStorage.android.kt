package com.nuvio.app.features.watchprogress

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

actual object ContinueWatchingPreferencesStorage {
    private const val preferencesName = "nuvio_continue_watching_preferences"
    private const val payloadKey = "continue_watching_preferences_payload"

    private var preferences: SharedPreferences? = null
    private val inMemoryFallback = mutableMapOf<String, String>()

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadPayload(): String? {
        val key = ProfileScopedKey.of(payloadKey)
        return preferences?.getString(key, null) ?: inMemoryFallback[key]
    }

    actual fun savePayload(payload: String) {
        val key = ProfileScopedKey.of(payloadKey)
        val prefs = preferences
        if (prefs == null) {
            inMemoryFallback[key] = payload
            return
        }
        prefs
            .edit()
            .putString(key, payload)
            .apply()
    }
}
