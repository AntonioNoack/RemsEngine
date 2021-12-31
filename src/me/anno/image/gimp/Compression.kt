package me.anno.image.gimp

enum class Compression {

    NONE,
    RLE,
    ZLIB, // unused, the code says
    FRACTAL; // unused, the code says

    companion object {
        val values2 = values()
    }

}