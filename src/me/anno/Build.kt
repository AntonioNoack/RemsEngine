package me.anno

object Build {
    var isDebug = true
        set(value) {
            if (!isLocked) field = value
        }
    var isShipped = false
        set(value) {
            if (!isLocked) field = value
        }
    private var isLocked = false
    @Suppress("unused")
    fun lock() {
        // probably not save because of Java reflections, but whatever
        isLocked = true
    }
}