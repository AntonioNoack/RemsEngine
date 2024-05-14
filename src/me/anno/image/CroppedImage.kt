package me.anno.image

class CroppedImage(val self: Image, x0: Int, y0: Int, w0: Int, h0: Int) :
    Image(w0, h0, self.numChannels, self.hasAlphaChannel, self.getIndex(x0, y0), self.stride) {
    override fun getRGB(index: Int): Int {
        return self.getRGB(index)
    }
}