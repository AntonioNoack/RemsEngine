package me.anno

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
    @Suppress("unused")
    fun lock() {
        // probably not save because of Java reflections, but whatever
        isLocked = true
    }
}