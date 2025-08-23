package me.anno.video

import me.anno.cache.ICacheData
import me.anno.video.formats.gpu.GPUFrame

class VideoSlice(
    val key: VideoFramesKey,
    val w: Int, val h: Int,
    val originalWidth: Int, // meta?.videoWidth
    val originalFPS: Double, // meta?.videoFPS ?: 0.0001
    val numTotalFramesInSrc: Int,
) : ICacheData {

    val frames = ArrayList<GPUFrame>()
    var isFinished = false

    fun getFrame(localIndex: Int, needsToBeCreated: Boolean): GPUFrame? {
        val frame = frames.getOrNull(localIndex)
        return if (!needsToBeCreated || frame?.isCreated == true) frame else null
    }

    var isDestroyed = false
    override fun destroy() {
        isDestroyed = true
        for (frame in frames) {
            frame.destroy()
        }
    }
}