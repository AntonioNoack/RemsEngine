package me.anno.engine

import me.anno.io.utils.StringMap
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc

enum class GFXSettings(
    val id: Int,
    val nameDesc: NameDesc
) {
    LOW(1, NameDesc("Low","", "ui.gfxSettings.low")),
    MEDIUM(2, NameDesc("Medium","", "ui.gfxSettings.medium")),
    HIGH(3, NameDesc("High","", "ui.gfxSettings.high"));

    val data = StringMap()

    operator fun get(key: String): Any? = data[key]

    companion object {

        @JvmStatic
        fun get(id: Int, ifMissing: GFXSettings) = entries.firstOrNull { it.id == id } ?: ifMissing

        @JvmStatic
        fun <V> put(key: String, low: V, medium: V, high: V) {
            LOW.data[key] = low
            MEDIUM.data[key] = medium
            HIGH.data[key] = high
        }

        init {
            // small frames per container are theoretically good, but practically,
            // with a lof of videos (I tested 4x 1080p @ 30fps), we need to prefetch more
            put("ui.editor.useMSAA", low = false, medium = true, high = true)
            put("video.frames.perContainer", 32, 64, 128)
        }
    }
}