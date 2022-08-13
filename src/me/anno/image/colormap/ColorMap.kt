package me.anno.image.colormap

/**
 * color maps can be used to visualize grayscale images better
 * */
interface ColorMap {

    val min: Float
    val max: Float

    val hasAlpha: Boolean

    fun getColor(v: Float): Int

    fun clone(min: Float, max: Float): ColorMap

    fun normalized(values: FloatArray): ColorMap {
        var min = 0f
        var max = 0f
        for (i in values.indices) {
            val v = values[i]
            if (v.isFinite()) {
                if (v < min) min = v
                if (v > max) max = v
            }
        }
        return clone(min, max)
    }

}