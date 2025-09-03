package me.anno.cache

import me.anno.Time
import me.anno.maths.Maths.clamp

/**
 * time in milliseconds;
 * will only be incremented once per frame,
 * and at most 16ms to prevent lag spikes from clearing the cache
 *
 * -> this will likely lag behind true time
 * */
object CacheTime {

    var cacheTimeMillis = 0L
        private set

    private var lastUpdateFrameIndex = 0
    const val MAX_CACHE_DT_MILLIS = 16

    fun updateTime() {
        val currFrameIndex = Time.frameIndex
        if (currFrameIndex == lastUpdateFrameIndex) return

        // 1 ms is ok: if anyone is running at more than 1000 fps their cache might empty a bit faster ^^
        lastUpdateFrameIndex = currFrameIndex
        cacheTimeMillis += clamp((Time.rawDeltaTime * 1000.0).toInt(), 1, MAX_CACHE_DT_MILLIS)
    }

}