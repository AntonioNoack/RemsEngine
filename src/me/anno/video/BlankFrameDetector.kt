package me.anno.video

import me.anno.cache.instances.VideoCache
import me.anno.io.files.FileReference
import me.anno.maths.Maths
import me.anno.utils.Sleep.waitUntilDefined
import me.anno.video.ffmpeg.FFMPEGMetadata
import me.anno.video.formats.gpu.GPUFrame
import org.apache.logging.log4j.LogManager
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * comparison information between frames to detect frames,
 * which don't belong in the video, e.g. black/white frame
 * todo even without debug mode, visualize these blank frames
 * */
class BlankFrameDetector {

    private val pixels = IntArray(samples)

    fun putRGBA(data: ByteBuffer) {
        val ris = randomIndexSequence
        val size = data.remaining() / 4
        for (i in ris.indices) {
            val pixelIndex = (size * ris[i]).toInt()
            val rgba = data.getInt(pixelIndex * 4)
            pixels[i] = pixels[i] or rgba
        }
    }

    fun putChannel(data: ByteBuffer, channel: Int) {
        val shift = channel * 8
        val ris = randomIndexSequence
        val size = data.remaining()
        for (i in ris.indices) {
            val pixelIndex = (size * ris[i]).toInt()
            val byte = data[pixelIndex]
            pixels[i] = pixels[i] or byte.toInt().and(255).shl(shift)
        }
    }

    private fun isBlankFrameR(f0: Int, f2: Int, f4: Int): Int {
        // detects too many frames, we need to filter only the central one
        val min = min(f0, f4)
        val max = max(f0, f4)
        return if (f2 in min..max) 0 else 1
    }

    private fun isBlankFrameRGBA(f0: Int, f2: Int, f4: Int): Int {
        val m0 = 0xff
        val m1 = 0xff00
        val m2 = 0xff0000
        val s3 = 24
        var sum = 0
        sum += isBlankFrameR(f0.and(m0), f2.and(m0), f4.and(m0))
        sum += isBlankFrameR(f0.and(m1), f2.and(m1), f4.and(m1))
        sum += isBlankFrameR(f0.and(m2), f2.and(m2), f4.and(m2))
        sum += isBlankFrameR(f0.ushr(s3), f2.ushr(s3), f4.ushr(s3))
        return sum
    }

    fun isBlankFrame(
        frame0: BlankFrameDetector,
        frame4: BlankFrameDetector,
        outlierThreshold: Float = 1f
    ): Boolean {
        if (outlierThreshold > 0f) {
            val p0 = frame0.pixels
            val p2 = pixels
            val p4 = frame4.pixels
            var outliers = 0
            val threshold = (samples * outlierThreshold).toInt()
            if (threshold == 0) return false
            for (i in 0 until samples) {
                outliers += isBlankFrameRGBA(p0[i], p2[i], p4[i])
                if (outliers >= threshold) {
                    if (!hasWarned) {
                        hasWarned = true
                        LOGGER.debug("Frame has error $outliers >= $threshold")
                    }
                    return true
                }
            }
        }
        return false
    }

    private var hasWarned = false

    companion object {

        private val LOGGER = LogManager.getLogger(BlankFrameDetector::class)

        private const val samples = 250
        private val randomIndexSequence = FloatArray(samples) { Maths.random().toFloat() }
            .apply { sort() }

        fun getFrame(
            src: FileReference, scale: Int, frameIndex: Int, bufferSize: Int, fps: Double,
            timeout: Long, meta: FFMPEGMetadata, async: Boolean,
            threshold: Float = 1f
        ): GPUFrame? {

            fun getFrame(delta: Int): GPUFrame? {
                return if (async) {
                    VideoCache.getVideoFrame(src, scale, frameIndex + delta, bufferSize, fps, timeout, meta, async)
                } else {
                    waitUntilDefined(true) {
                        VideoCache.getVideoFrame(src, scale, frameIndex + delta, bufferSize, fps, timeout, meta, true)
                    }
                }
            }

            val f0 = getFrame(-3)
            val f1 = getFrame(-2)
            val f2 = getFrame(-1)
            val f3 = getFrame(+0)
            val f4 = getFrame(+1)
            val f5 = getFrame(+2)

            return when {
                f3 != null && f1 != null && f5 != null && f0 != null && f2 != null && f4 != null -> {
                    if (f3.isBlankFrame(f1, f5, threshold)) {
                        if (f2.isBlankFrame(f0, f4, threshold)) f4 else f2
                    } else f3
                }
                else -> f3
            }

        }

        @Suppress("unused")
        fun isBlankFrame(
            src: FileReference, scale: Int,
            frameIndex: Int, bufferSize: Int,
            fps: Double,
            threshold: Float = 1f
        ): Boolean? {

            fun getFrame(delta: Int): GPUFrame? {
                val frameIndex2 = frameIndex + delta
                val bufferIndex = frameIndex2 / bufferSize
                return VideoCache.getFrameWithoutGenerator(src, scale, frameIndex2, bufferIndex, bufferSize, fps)
            }

            val f1 = getFrame(-2)
            val f3 = getFrame(+0)
            val f5 = getFrame(+2)

            return when {
                f3 != null && f1 != null && f5 != null ->
                    f3.isBlankFrame(f1, f5, threshold)
                else -> null
            }

        }

        fun isBlankFrame2(
            src: FileReference, scale: Int,
            frameIndex: Int, bufferSize: Int,
            fps: Double,
            threshold: Float = 1f
        ): Boolean {
            return if (threshold > 0f) {

                fun getFrame(delta: Int): GPUFrame? {
                    val frameIndex2 = frameIndex + delta
                    val bufferIndex = frameIndex2 / bufferSize
                    return VideoCache.getFrameWithoutGenerator(src, scale, frameIndex2, bufferIndex, bufferSize, fps)
                }

                val f1 = getFrame(-2)
                val f3 = getFrame(+0)
                val f5 = getFrame(+2)

                when {
                    f3 != null && f1 != null && f5 != null ->
                        f3.isBlankFrame(f1, f5, threshold)
                    else -> false
                }

            } else false
        }
    }
}