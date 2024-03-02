package me.anno.image

import org.joml.Matrix4f
import kotlin.math.PI
import kotlin.math.abs

/**
 * Image files with EXIF metadata can be stored differently to what they should be displayed at.
 * This class stores possible transforms like that.
 * */
class ImageTransform(
    /**
     * whether the image needs to be mirrored
     * */
    val mirrorHorizontal: Boolean, val mirrorVertical: Boolean,
    /**
     * angle, clock-wise, in degrees
     * */
    val angleCW: Int
) {
    @Suppress("unused")
    val switchWH get() = (abs(angleCW) % 180) > 45
    private val angleRadians = -(PI * angleCW / 180).toFloat() // CCW
    fun apply(stack: Matrix4f) {
        if (mirrorHorizontal) {
            stack.scale(1f, -1f, 1f)
        } else if (mirrorVertical) {
            stack.scale(-1f, 1f, 1f)
        }
        if (angleCW != 0) {
            stack.rotateZ(angleRadians)
        }
    }

    fun isNull() = !mirrorHorizontal && !mirrorVertical && angleCW == 0
    override fun toString() = "${if (mirrorHorizontal) "x" else ""}${if (mirrorVertical) "y" else ""}$angleCW"
}