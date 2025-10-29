package me.anno.image

class ArrayImage(
    val images: List<Image>,
    val isY: Boolean,
    private val sample: Image = images[0]
) : Image(
    sample.width * if (isY) 1 else images.size,
    sample.height * if (isY) images.size else 1,
    sample.numChannels, sample.hasAlphaChannel
) {
    override fun getRGB(index: Int): Int {
        val x = index % width
        val y = index / width
        val lx = if (isY) x else x % sample.width
        val ly = if (isY) y % sample.height else y
        val ii = if (isY) y / sample.height else x / sample.width
        return images[ii].getRGB(lx, ly)
    }

    override fun setRGB(index: Int, value: Int) {
        val x = index % width
        val y = index / width
        val lx = if (isY) x else x % sample.width
        val ly = if (isY) y % sample.height else y
        val ii = if (isY) y / sample.height else x / sample.width
        images[ii].setRGB(lx, ly, value)
    }
}