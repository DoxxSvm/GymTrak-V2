package `in`.gym.trak.studio

interface Platform {
    val name: String
    
    fun setBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun setString(key: String, value: String)
    fun getString(key: String, defaultValue: String): String
    fun setLong(key: String, value: Long)
    fun getLong(key: String, defaultValue: Long): Long
    /** Removes a key from persistent storage (SharedPreferences / UserDefaults). */
    fun remove(key: String)
}

expect fun getPlatform(): Platform

expect fun getCurrentTimeMillis(): Long
