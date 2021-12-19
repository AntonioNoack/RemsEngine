package me.anno.ui.base

import me.anno.image.ImageGPUCache.getInternalTexture
import me.anno.ui.style.Style

open class IconPanel(val texturePath: String, style: Style) : ImagePanel(style) {

    override fun getTexture() = getInternalTexture(texturePath, true)

}