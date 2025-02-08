package me.anno.ui.base.image

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.texture.TextureCache
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.ui.Style

open class IconPanel(var source: FileReference, style: Style) : ImagePanel(style) {

    @Suppress("unused")
    constructor(style: Style) : this(InvalidRef, style)

    override fun getTexture() = TextureCache[source, true]

    override fun clone(): IconPanel {
        val clone = IconPanel(source, style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is IconPanel) return
        dst.source = source
    }
}