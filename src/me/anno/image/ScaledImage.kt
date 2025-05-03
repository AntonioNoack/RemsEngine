package me.anno.image

import org.joml.Vector4f

class ScaledImage(val src: Image, val sx: Int, val sy: Int) :
    Image(src.width * sx, src.height * sy, src.numChannels, src.hasAlphaChannel) {

    private fun toSelfIndex(index: Int): Int {
        val width = width
        val x = (index % width) / sx
        val y = (index / width) / sy
        return src.getIndex(x, y)
    }

    override fun getRGB(index: Int): Int {
        return src.getRGB(toSelfIndex(index))
    }

    override fun setRGB(index: Int, value: Int) {
        src.setRGB(toSelfIndex(index), value)
    }

    override fun setRGB(index: Int, value: Vector4f) {
        src.setRGB(toSelfIndex(index), value)
    }
}