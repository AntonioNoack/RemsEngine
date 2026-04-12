package me.anno.image.qoi

import me.anno.image.raw.IntImage

/**
 * should we convert to SRGB, if linear? typically yes, except for normals -> just keep it default
 * */
class QOIImage(width: Int, height: Int, numChannels: Int, val linear: Boolean, data: IntArray) :
    IntImage(width, height, data, numChannels > 3) {
}