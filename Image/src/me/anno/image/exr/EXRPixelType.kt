package me.anno.image.exr

enum class EXRPixelType(val bytesPerPixel: Int) {
    UINT32(4),
    HALF(2),
    FLOAT(4),
}
