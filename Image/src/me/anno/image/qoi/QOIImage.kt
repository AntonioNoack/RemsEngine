package me.anno.image.qoi

import me.anno.image.raw.IntImage
import me.anno.io.Streams.readBE32
import me.anno.utils.structures.tuples.IntPair
import java.io.IOException
import java.io.InputStream

/**
 * should we convert to SRGB, if linear? typically yes, except for normals -> just keep it default
 * */
class QOIImage(width: Int, height: Int, channels: Int, val linear: Boolean, data: IntArray) :
    IntImage(width, height, data, channels > 3) {
}