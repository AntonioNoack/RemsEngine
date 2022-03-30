package me.anno.image.raw

import me.anno.image.Image

/**
 * the easiest check whether an image has R and B channels inverted: if so, this will look correct
 * */
class BGRAImage(val base: Image) :
    Image(base.width, base.height, base.numChannels, base.hasAlphaChannel) {

    override var width: Int
        get() = base.width
        set(value) {
            base.width = value
        }

    override var height: Int
        get() = base.height
        set(value) {
            base.height = value
        }

    override fun getRGB(index: Int): Int {
        // argb -> abgr
        val c = base.getRGB(index)
        return c.and(0xff00ff00.toInt()) or
                c.and(0xff).shl(16) or
                c.shr(16).and(0xff)
    }

}