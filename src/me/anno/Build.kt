package me.anno

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import org.apache.logging.log4j.LogManager

object Build {

    private val LOGGER = LogManager.getLogger(Build::class)

    @JvmStatic
    var isDebug = true
        set(value) {
            if (!isLocked) field = value
            else LOGGER.warn("Cannot set isDebug when locked")
        }

    @JvmStatic
    var isShipped = false
        set(value) {
            if (!isLocked) field = value
            else LOGGER.warn("Cannot set isShipped when locked")
        }

    @JvmStatic
    var isLocked = false
        private set

    @JvmStatic // who is using this?
    var assetsFolder: FileReference = InvalidRef

    /**
     * locks the properties "isDebug", "isShipped" and "isLocked", so they no longer can be changed (e.g., by mods);
     * probably not save because of Java reflections, but whatever, user-code is always unsafe
     * */
    @JvmStatic
    @Suppress("unused")
    fun lock() {
        // probably not save because of Java reflections, but whatever
        isLocked = true
    }
}