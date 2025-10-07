package me.anno.fonts.signeddistfields

import me.anno.cache.ICacheData

class TextSDFList(val content: List<TextSDF>) : ICacheData {
    override fun destroy() {
        for (i in content.indices) {
            content[i].destroy()
        }
    }
}