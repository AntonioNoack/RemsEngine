package me.anno.video

import me.anno.cache.AsyncCacheData
import me.anno.cache.GetterCacheContent
import me.anno.cache.ICacheData
import me.anno.maths.Maths.clamp
import me.anno.utils.Sleep
import me.anno.utils.structures.lists.SimpleList
import me.anno.video.formats.gpu.GPUFrame

class VideoSlice(
    val key: VideoFramesKey,
    val w: Int, val h: Int,
    val originalWidth: Int, // meta?.videoWidth
    val originalFPS: Double, // meta?.videoFPS ?: 0.0001
    val numTotalFramesInSrc: Int,
    self: AsyncCacheData<VideoSlice>
) : SimpleList<AsyncCacheData<GPUFrame>>(), ICacheData {

    val frames = run {
        val numFrames = clamp(
            numTotalFramesInSrc - key.bufferIndex * key.bufferLength,
            0, key.bufferLength
        )
        arrayOfNulls<GPUFrame>(numFrames)
    }

    private val asyncFrames = Array(frames.size) { index ->
        val data = object : GetterCacheContent<GPUFrame, VideoSlice>(self.content, { frames[index] }) {
            override val hasValue: Boolean
                get() = value != null || hasFinished

            /**
             * directly attaching a callback is not possible,
             * because self.hasValue will be immediately true
             * */
            override fun addCallback(callback: (GPUFrame?) -> Unit) {
                Sleep.waitUntil(true, { hasValue || hasBeenDestroyed }) {
                    callback(value)
                }
            }
        }
        AsyncCacheData(data)
    }

    override val size: Int get() = frames.size
    override fun get(index: Int): AsyncCacheData<GPUFrame> =
        asyncFrames.getOrNull(index) ?: AsyncCacheData.empty()

    var hasFinished = false

    override fun destroy() {
        hasFinished = true
        for (frame in frames) {
            frame?.destroy()
        }
        frames.fill(null)
    }
}