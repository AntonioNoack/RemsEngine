package me.anno.image.raw

class CroppedFloatImage(val self: IFloatImage, x0: Int, y0: Int, w0: Int, h0: Int) :
    IFloatImage(w0, h0, self.numChannels, self.map, self.getIndex(x0, y0), self.stride) {
    override fun getRGB(index: Int): Int {
        return self.getRGB(index)
    }

    override fun getValue(index: Int, channel: Int): Float {
        return self.getValue(index, channel)
    }

    override fun setValue(index: Int, channel: Int, value: Float): Float {
        return self.setValue(index, channel, value)
    }
}