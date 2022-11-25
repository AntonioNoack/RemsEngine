package me.anno

object Build {
    @JvmStatic
    var isDebug = true
        set(value) {
            if (!isLocked) field = value
        }
    @JvmStatic
    var isShipped = false
        set(value) {
            if (!isLocked) field = value
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