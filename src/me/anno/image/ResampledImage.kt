package me.anno.image

class ResampledImage(val src: Image, width: Int, height: Int) :
    Image(width, height, src.numChannels, src.hasAlphaChannel) {

    override fun getRGB(index: Int): Int {
        val x = index % width
        val y = index / width
        val xi = x * src.width / width
        val yi = y * src.height / height
        return src.getRGB(xi, yi)
    }

    override fun setRGB(index: Int, value: Int) {
        val x = index % width
        val y = index / width
        val xi = x * src.width / width
        val yi = y * src.height / height
        src.setRGB(xi, yi, value)
    }
}