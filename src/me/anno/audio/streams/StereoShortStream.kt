package me.anno.audio.streams

fun interface StereoShortStream {
    fun getBuffer(
        bufferSize: Int,
        time0: Double,
        time1: Double
    ): Pair<ShortArray, ShortArray>
}