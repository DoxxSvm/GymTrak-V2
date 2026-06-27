package `in`.gym.trak.studio

import android.content.Context

class AndroidPlatform : Platform {
    override val name: String = "Android"

    private val prefs = appContext?.getSharedPreferences("gym_prefs", Context.MODE_PRIVATE)

    override fun setBoolean(key: String, value: Boolean) {
        prefs?.edit()?.putBoolean(key, value)?.apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs?.getBoolean(key, defaultValue) ?: defaultValue
    }

    override fun setString(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    override fun getString(key: String, defaultValue: String): String {
        return prefs?.getString(key, defaultValue) ?: defaultValue
    }

    override fun setLong(key: String, value: Long) {
        prefs?.edit()?.putLong(key, value)?.apply()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return prefs?.getLong(key, defaultValue) ?: defaultValue
    }

    override fun remove(key: String) {
        prefs?.edit()?.remove(key)?.apply()
    }
}

var appContext: Context? = null

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
