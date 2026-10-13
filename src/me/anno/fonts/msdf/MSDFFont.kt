package me.anno.fonts.msdf

import me.anno.cache.ICacheData
import me.anno.gpu.texture.Texture2D
import me.anno.io.files.FileReference

class MSDFFont(
    val json: FileReference,
    val image: Texture2D,
) : ICacheData {
    override fun destroy() {
        image.destroy()
    }
}