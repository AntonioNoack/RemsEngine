package me.anno

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

object Build {

    @JvmStatic
    var isDebug = true
        set(value) {
            if (!isLocked) field = value
            else throw IllegalArgumentException("Cannot set debug to locked state")
        }

    @JvmStatic
    var isShipped = false
        set(value) {
            if (!isLocked) field = value
            else throw IllegalArgumentException("Cannot set shipped to locked state")
        }

    @JvmStatic
    private var isLocked = false

    @JvmStatic
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