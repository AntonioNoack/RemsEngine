package me.anno.audio

import me.anno.utils.pooling.FloatArrayPool
import me.anno.utils.pooling.ShortArrayPool

object AudioPools {
    // what are the ideal sizes? how large are single objects?
    // maybe I should use a capacity limit relative to the total RAM (?)
    val FAPool = FloatArrayPool(128)
    val SAPool = ShortArrayPool(64)

    fun gc() {
        FAPool.gc()
        SAPool.gc()
    }
}