package me.anno.image.raw

import me.anno.image.Image

/**
 * the easiest check whether an image has R and B channels inverted: if so, this will look correct
 * */
class BGRAImage(val base: Image):
    Image(base.width, base.height, base.numChannels, base.hasAlphaChannel) {

    override fun getWidth(): Int = base.getWidth()
    override fun getHeight(): Int = base.getHeight()

    override fun getRGB(index: Int): Int {
        // argb -> abgr
        val c = base.getRGB(index)
        return c.and(0xff00ff00.toInt()) or
                c.and(0xff).shl(16) or
                c.shr(16).and(0xff)
    }

}