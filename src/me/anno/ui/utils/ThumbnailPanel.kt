package me.anno.ui.utils

import me.anno.gpu.texture.ITexture2D
import me.anno.image.thumbs.ThumbnailCache
import me.anno.io.files.FileReference
import me.anno.ui.Style
import me.anno.ui.base.components.StretchModes
import me.anno.ui.base.image.ImagePanel
import kotlin.math.max
import kotlin.math.min

@Suppress("unused")
open class ThumbnailPanel(var source: FileReference, style: Style) : ImagePanel(style) {
    open fun getThumbnailResolution(): Int {
        return when (stretchMode) {
            StretchModes.STRETCH, StretchModes.OVERFLOW -> max(width, height)
            StretchModes.PADDING -> min(width, height)
        }
    }

    override fun getTexture(): ITexture2D? {
        return ThumbnailCache[source, getThumbnailResolution()]
    }
}