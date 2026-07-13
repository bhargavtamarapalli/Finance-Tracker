package com.example.fakes

import android.content.SharedPreferences

/**
 * In-memory [SharedPreferences] fake for use in unit and Robolectric tests **only**.
 *
 * This class lives exclusively in `src/test/`. It must NEVER be referenced from
 * production source (`src/main/`). Inject it wherever a test needs a
 * [SharedPreferences] instance without touching [EncryptedSharedPreferences] or
 * the Android Keystore.
 */
class FakeSharedPreferences : SharedPreferences {

    private val store = mutableMapOf<String, Any?>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): Map<String, *> = store.toMap()

    override fun getString(key: String, defValue: String?): String? =
        store.getOrDefault(key, defValue) as? String ?: defValue

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        @Suppress("UNCHECKED_CAST")
        store.getOrDefault(key, defValues) as? Set<String> ?: defValues

    override fun getInt(key: String, defValue: Int): Int =
        (store.getOrDefault(key, defValue) as? Int) ?: defValue

    override fun getLong(key: String, defValue: Long): Long =
        (store.getOrDefault(key, defValue) as? Long) ?: defValue

    override fun getFloat(key: String, defValue: Float): Float =
        (store.getOrDefault(key, defValue) as? Float) ?: defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        (store.getOrDefault(key, defValue) as? Boolean) ?: defValue

    override fun contains(key: String): Boolean = store.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.remove(listener)
    }

    inner class FakeEditor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor { pending[key] = value; return this }
        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor { pending[key] = values; return this }
        override fun putInt(key: String, value: Int): SharedPreferences.Editor { pending[key] = value; return this }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor { pending[key] = value; return this }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor { pending[key] = value; return this }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { pending[key] = value; return this }
        override fun remove(key: String): SharedPreferences.Editor { removals.add(key); return this }
        override fun clear(): SharedPreferences.Editor { clearAll = true; return this }

        override fun commit(): Boolean { apply(); return true }

        override fun apply() {
            if (clearAll) store.clear()
            removals.forEach { store.remove(it) }
            store.putAll(pending)
        }
    }
}
