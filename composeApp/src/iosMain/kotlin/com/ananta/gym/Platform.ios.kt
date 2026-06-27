package `in`.gym.trak.studio

import platform.UIKit.UIDevice
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSDate
import platform.Foundation.date
import platform.Foundation.timeIntervalSince1970

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun setBoolean(key: String, value: Boolean) {
        defaults.setBool(value, key)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return if (defaults.objectForKey(key) == null) {
            defaultValue
        } else {
            defaults.boolForKey(key)
        }
    }

    override fun setString(key: String, value: String) {
        defaults.setObject(value, key)
    }

    override fun getString(key: String, defaultValue: String): String {
        return defaults.stringForKey(key) ?: defaultValue
    }

    override fun setLong(key: String, value: Long) {
        defaults.setObject(value.toString(), forKey = key)
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return defaults.stringForKey(key)?.toLongOrNull() ?: defaultValue
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getCurrentTimeMillis(): Long = (NSDate.date().timeIntervalSince1970 * 1000).toLong()
