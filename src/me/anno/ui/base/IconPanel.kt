package me.anno.ui.base

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.image.ImageGPUCache.getInternalTexture
import me.anno.ui.style.Style

open class IconPanel(var internalPath: String, style: Style) : ImagePanel(style) {

    constructor(style: Style): this("", style)

    override fun getTexture() = getInternalTexture(internalPath, true)

    override fun clone(): IconPanel {
        val clone = IconPanel(internalPath, style)
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as IconPanel
        clone.internalPath = internalPath
    }

    override val className: String = "IconPanel"

}