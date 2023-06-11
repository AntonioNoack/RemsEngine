package me.anno.gpu.framebuffer

import me.anno.cache.CacheSection
import me.anno.cache.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.deferred.BufferQuality
import me.anno.gpu.framebuffer.TargetType.Companion.FP16Targets
import me.anno.gpu.framebuffer.TargetType.Companion.FloatTargets
import me.anno.gpu.framebuffer.TargetType.Companion.UByteTargets
import me.anno.maths.Maths.clamp
import org.apache.logging.log4j.LogManager

object FBStack : CacheSection("FBStack") {

    private val LOGGER = LogManager.getLogger(FBStack::class)
    private const val timeout = 2100L

    abstract class FBStackData(
        val w: Int,
        val h: Int,
        private val samples: Int,
        private val targetTypes: Array<TargetType>,
        private val withDepth: Boolean
    ) : ICacheData {

        var nextIndex = 0
        val data = ArrayList<Framebuffer>()

        override fun destroy() {
            if (data.isNotEmpty()) {
                for (it in data) {
                    it.destroy()
                }
                printDestroyed(data.size)
                data.clear()
            }
        }

        fun getFrame(name: String): Framebuffer {
            return if (nextIndex >= data.size) {
                val framebuffer = Framebuffer(
                    name, w, h,
                    samples, targetTypes,
                    if (withDepth) DepthBufferType.TEXTURE
                    else DepthBufferType.NONE
                )
                data.add(framebuffer)
                nextIndex = data.size
                data.last()
            } else {
                val framebuffer = data[nextIndex++]
                framebuffer.name = name
                framebuffer
            }
        }

        abstract fun printDestroyed(size: Int)

    }

    data class FBKey1(
        val w: Int,
        val h: Int,
        val channels: Int,
        val quality: BufferQuality,
        val samples: Int,
        val withDepth: Boolean
    )

    class FBStackData1(val key: FBKey1) :
        FBStackData(key.w, key.h, key.samples, arrayOf(getTargetType(key.channels, key.quality)), key.withDepth) {
        override fun printDestroyed(size: Int) {
            val fs = if (size == 1) "1 framebuffer" else "$size framebuffers"
            LOGGER.info("Freed $fs of size ${key.w} x ${key.h}, samples: ${key.samples}, quality: ${key.quality}")
        }
    }

    data class FBKey2(val w: Int, val h: Int, val targetType: TargetType, val samples: Int, val withDepth: Boolean)
    data class FBKey3(
        val w: Int,
        val h: Int,
        val targetTypes: Array<TargetType>,
        val samples: Int,
        val withDepth: Boolean
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as FBKey3

            if (w != other.w) return false
            if (h != other.h) return false
            if (!targetTypes.contentEquals(other.targetTypes)) return false
            if (samples != other.samples) return false
            if (withDepth != other.withDepth) return false

            return true
        }

        override fun hashCode(): Int {
            var result = w
            result = 31 * result + h
            result = 31 * result + targetTypes.contentHashCode()
            result = 31 * result + samples
            result = 31 * result + withDepth.hashCode()
            return result
        }
    }

    class FBStackData2(val key: FBKey2) :
        FBStackData(key.w, key.h, key.samples, arrayOf(key.targetType), key.withDepth) {
        override fun printDestroyed(size: Int) {
            val fs = if (size == 1) "1 framebuffer" else "$size framebuffers"
            LOGGER.info("Freed $fs of size ${key.w} x ${key.h}, samples: ${key.samples}, type: ${key.targetType}")
        }
    }

    class FBStackData3(val key: FBKey3) : FBStackData(key.w, key.h, key.samples, key.targetTypes, key.withDepth) {
        override fun printDestroyed(size: Int) {
            val fs = if (size == 1) "1 framebuffer" else "$size framebuffers"
            LOGGER.info("Freed $fs of size ${key.w} x ${key.h}, samples: ${key.samples}, type: ${key.targetTypes}")
        }
    }

    fun getValue(w: Int, h: Int, channels: Int, quality: BufferQuality, samples: Int, withDepth: Boolean): FBStackData {
        val key = FBKey1(w, h, channels, quality, clamp(samples, 1, GFX.maxSamples), withDepth)
        return getEntry(key, timeout, false) {
            FBStackData1(it)
        } as FBStackData
    }

    fun getValue(w: Int, h: Int, targetType: TargetType, samples: Int, withDepth: Boolean): FBStackData {
        val key = FBKey2(w, h, targetType, clamp(samples, 1, GFX.maxSamples), withDepth)
        return getEntry(key, timeout, false) {
            FBStackData2(it)
        } as FBStackData
    }

    fun getValue(w: Int, h: Int, targetTypes: Array<TargetType>, samples: Int, withDepth: Boolean): FBStackData {
        val key = FBKey3(w, h, targetTypes, clamp(samples, 1, GFX.maxSamples), withDepth)
        return getEntry(key, timeout, false) {
            FBStackData3(it)
        } as FBStackData
    }

    operator fun get(
        name: String, w: Int, h: Int, channels: Int,
        quality: BufferQuality, samples: Int, withDepth: Boolean
    ): Framebuffer {
        val value = getValue(w, h, channels, quality, samples, withDepth)
        synchronized(value) {
            return value.getFrame(name)
        }
    }

    operator fun get(
        name: String, w: Int, h: Int, channels: Int,
        fp: Boolean, samples: Int, withDepth: Boolean
    ): Framebuffer {
        val quality = if (fp) BufferQuality.HIGH_32 else BufferQuality.LOW_8
        return get(name, w, h, channels, quality, samples, withDepth)
    }

    operator fun get(
        name: String, w: Int, h: Int,
        targetType: TargetType, samples: Int, withDepth: Boolean
    ): Framebuffer {
        val value = getValue(w, h, targetType, samples, withDepth)
        synchronized(value) {
            return value.getFrame(name)
        }
    }

    operator fun get(
        name: String, w: Int, h: Int,
        targetTypes: Array<TargetType>, samples: Int, withDepth: Boolean
    ): Framebuffer {
        val value = getValue(w, h, targetTypes, samples, withDepth)
        synchronized(value) {
            return value.getFrame(name)
        }
    }

    fun getTargetType(channels: Int, quality: BufferQuality): TargetType {
        return when (quality) {
            BufferQuality.LOW_8 -> UByteTargets
            BufferQuality.MEDIUM_12, BufferQuality.HIGH_16 -> FP16Targets
            BufferQuality.HIGH_32 -> FloatTargets
        }[channels - 1]
    }

    fun reset(w: Int, h: Int) {
        synchronized(cache) {
            for (value in cache.values) {
                val data = value.data
                if (data is FBStackData && data.w == w && data.h == h) {
                    data.nextIndex = 0
                }
            }
        }
    }

    fun reset() {
        resetFBStack()
    }

    @Suppress("JavaMapForEach")
    private fun resetFBStack() {
        synchronized(cache) {
            for (v in cache.values) {
                val data = v.data
                if (data is FBStackData) {
                    data.nextIndex = 0
                }
            }
        }
    }

}