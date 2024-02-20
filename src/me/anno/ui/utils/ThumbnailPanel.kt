package me.anno.ui.utils

import me.anno.gpu.texture.ITexture2D
import me.anno.io.files.FileReference
import me.anno.image.thumbs.Thumbs
import me.anno.maths.Maths
import me.anno.ui.Style
import me.anno.ui.base.ImagePanel
import me.anno.ui.base.components.StretchModes

@Suppress("unused")
open class ThumbnailPanel(var source: FileReference, style: Style) : ImagePanel(style) {
    open fun getThumbnailResolution(): Int {
        return when (stretchMode) {
            StretchModes.STRETCH, StretchModes.OVERFLOW -> Maths.max(width, height)
            StretchModes.PADDING -> Maths.min(width, height)
        }
    }

    override fun getTexture(): ITexture2D? {
        return Thumbs[source, getThumbnailResolution(), true]
    }
}