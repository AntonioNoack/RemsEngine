package me.anno.studio

import me.anno.io.utils.StringMap

enum class GFXSettings(val id: Int, val displayName: String) {
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3,"High");

    val data = StringMap()

    operator fun get(key: String) = data[key] as Boolean

    companion object {
        fun put(key: String, low: Boolean, medium: Boolean, high: Boolean){
            LOW.data[key] = low
            MEDIUM.data[key] = medium
            HIGH.data[key] = high
        }
        init {
            put("editor.useMSAA", false, true, true)

        }
    }
}