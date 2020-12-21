package me.anno.ui.base

import me.anno.cache.Cache
import me.anno.ui.style.Style

open class IconPanel(var name: String, style: Style): ImagePanel(style){

    override fun getTexture() = Cache.getInternalTexture(name, true)

}