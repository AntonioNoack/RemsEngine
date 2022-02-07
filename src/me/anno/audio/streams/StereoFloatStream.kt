package me.anno.audio.streams

interface StereoFloatStream {

    fun getBuffer(
        bufferSize: Int,
        time0: Double,
        time1: Double
    ): Pair<FloatArray, FloatArray>

}