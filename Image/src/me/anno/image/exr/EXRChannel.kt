package me.anno.image.exr

class EXRChannel {
    lateinit var name: String
    lateinit var pixType: EXRPixelType
    var linear: Byte = 0
    var xSampling: Int = 0
    var ySampling: Int = 0

    override fun toString(): String {
        return String.format(
            "[name=%s, type=%d, linear=%d, xsamp=%d, ysamp=%d]",
            name, pixType, linear, xSampling, ySampling
        )
    }
}
