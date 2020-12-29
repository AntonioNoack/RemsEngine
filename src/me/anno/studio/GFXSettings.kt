package me.anno.studio

import me.anno.io.utils.StringMap

enum class GFXSettings(val id: Int, val displayName: String) {
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3,"High");

    val data = StringMap()

    operator fun get(key: String) = data[key] as Boolean
    fun getInt(key: String) = data[key] as Int
    fun getFloat(key: String) = data[key] as Float
    fun getDouble(key: String) = data[key] as Double

    companion object {
        fun get(id: Int, alternative: GFXSettings) = values().firstOrNull { it.id == id } ?: alternative
        fun <V> put(key: String, low: V, medium: V, high: V){
            LOW.data[key] = low
            MEDIUM.data[key] = medium
            HIGH.data[key] = high
        }
        init {
            put("ui.editor.useMSAA", false, true, true)
            put("video.frames.perContainer", 32, 128, 512)
        }
    }
}