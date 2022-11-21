package me.anno.studio

import me.anno.io.utils.StringMap
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc

@Suppress("unused")
enum class GFXSettings(
    val id: Int,
    private val displayNameEn: String,
    private val displayNameDict: String) {
    LOW(1, "Low", "ui.gfxSettings.low"),
    MEDIUM(2, "Medium", "ui.gfxSettings.medium"),
    HIGH(3,"High", "ui.gfxSettings.high");

    val naming = NameDesc(displayNameEn, "", displayNameDict)

    val displayName get() = Dict[displayNameEn, displayNameDict]
    val data = StringMap()

    operator fun get(key: String) = data[key] as Boolean
    fun getInt(key: String) = data[key] as Int
    fun getFloat(key: String) = data[key] as Float
    fun getDouble(key: String) = data[key] as Double

    companion object {
        @JvmStatic
        fun get(id: Int, alternative: GFXSettings) = values().firstOrNull { it.id == id } ?: alternative
        @JvmStatic
        fun <V> put(key: String, low: V, medium: V, high: V){
            LOW.data[key] = low
            MEDIUM.data[key] = medium
            HIGH.data[key] = high
        }
        init {
            // small frames per container are theoretically good, but practially,
            // with a lof of videos (I tested 4x 1080p @ 30fps), we need to prefetch more
            put("ui.editor.useMSAA", false, true, true)
            put("video.frames.perContainer", 32, 64, 128)
        }
    }
}