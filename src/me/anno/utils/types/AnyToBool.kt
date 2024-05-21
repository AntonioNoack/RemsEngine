package me.anno.utils.types

object AnyToBool {
    fun anyToBool(v: Any?): Boolean {
        return when (v) {
            is Boolean -> v
            is Int -> v != 0
            is Long -> v != 0L
            is Float -> v.isFinite() && v != 0f
            is Double -> v.isFinite() && v != 0.0
            "1", "t", "true" -> true
            is String -> false
            else -> v != null
        }
    }
}