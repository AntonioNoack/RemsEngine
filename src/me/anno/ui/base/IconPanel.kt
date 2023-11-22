package me.anno.ui.base

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.texture.TextureCache
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.ui.Style

open class IconPanel(var internalPath: FileReference, style: Style) : ImagePanel(style) {

    constructor(style: Style) : this(InvalidRef, style)

    override fun getTexture() = TextureCache[internalPath, true]

    override fun clone(): IconPanel {
        val clone = IconPanel(internalPath, style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as IconPanel
        dst.internalPath = internalPath
    }

    override val className: String get() = "IconPanel"

}