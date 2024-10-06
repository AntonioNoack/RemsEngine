package me.anno.image

import java.io.OutputStream

fun interface ImageStreamWriter {
    fun write(image: Image, output: OutputStream, format: String, quality: Float)
}