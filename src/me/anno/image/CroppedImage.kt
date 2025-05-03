package me.anno.image

class CroppedImage(val src: Image, x0: Int, y0: Int, w0: Int, h0: Int) :
    Image(w0, h0, src.numChannels, src.hasAlphaChannel, src.getIndex(x0, y0), src.stride) {

    override fun getRGB(index: Int): Int {
        return src.getRGB(index)
    }

    override fun setRGB(index: Int, value: Int) {
        src.setRGB(index, value)
    }
}