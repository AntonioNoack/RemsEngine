package me.anno.gpu.framebuffer

import me.anno.cache.CacheSection
import me.anno.cache.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.deferred.BufferQuality
import me.anno.maths.Maths.clamp
import org.apache.logging.log4j.LogManager

object FBStack : CacheSection("FBStack") {

    private val LOGGER = LogManager.getLogger(FBStack::class)
    private const val timeout = 2100L

    abstract class FBStackData(
        val w: Int,
        val h: Int,
        private val samples: Int,
        private val targetType: TargetType,
        private val withDepth: Boolean
    ) : ICacheData {

        var nextIndex = 0
        val data = ArrayList<Framebuffer>()

        override fun destroy() {
            if (data.isNotEmpty()) {
                data.forEach { it.destroy() }
                printDestroyed(data.size)
                data.clear()
            }
        }

        fun getFrame(name: String): Framebuffer {
            return if (nextIndex >= data.size) {
                val framebuffer = Framebuffer(
                    name, w, h,
                    samples, arrayOf(targetType),
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
        FBStackData(key.w, key.h, key.samples, getTargetType(key.channels, key.quality), key.withDepth) {
        override fun printDestroyed(size: Int) {
            val fs = if (size == 1) "1 framebuffer" else "$size framebuffers"
            LOGGER.info("Freed $fs of size ${key.w} x ${key.h}, samples: ${key.samples}, quality: ${key.quality}")
        }
    }

    data class FBKey2(val w: Int, val h: Int, val targetType: TargetType, val samples: Int, val withDepth: Boolean)
    class FBStackData2(val key: FBKey2) : FBStackData(key.w, key.h, key.samples, key.targetType, key.withDepth) {
        override fun printDestroyed(size: Int) {
            val fs = if (size == 1) "1 framebuffer" else "$size framebuffers"
            LOGGER.info("Freed $fs of size ${key.w} x ${key.h}, samples: ${key.samples}, type: ${key.targetType}")
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

    operator fun get(
        name: String,
        w: Int,
        h: Int,
        channels: Int,
        quality: BufferQuality,
        samples: Int,
        withDepth: Boolean
    ): Framebuffer {
        val value = getValue(w, h, channels, quality, samples, withDepth)
        synchronized(value) {
            return value.getFrame(name)
        }
    }

    operator fun get(
        name: String,
        w: Int,
        h: Int,
        channels: Int,
        fp: Boolean,
        samples: Int,
        withDepth: Boolean
    ): Framebuffer {
        val quality = if (fp) BufferQuality.HIGH_32 else BufferQuality.LOW_8
        return get(name, w, h, channels, quality, samples, withDepth)
    }

    operator fun get(
        name: String,
        w: Int,
        h: Int,
        targetType: TargetType,
        samples: Int,
        withDepth: Boolean
    ): Framebuffer {
        val value = getValue(w, h, targetType, samples, withDepth)
        synchronized(value) {
            return value.getFrame(name)
        }
    }

    fun getTargetType(channels: Int, quality: BufferQuality): TargetType {
        return when (quality) {
            BufferQuality.LOW_8 -> {
                when (channels) {
                    1 -> TargetType.UByteTarget1
                    2 -> TargetType.UByteTarget2
                    3 -> TargetType.UByteTarget3
                    else -> TargetType.UByteTarget4
                }
            }
            BufferQuality.MEDIUM_12, BufferQuality.HIGH_16 -> {
                when (channels) {
                    1 -> TargetType.FP16Target1
                    2 -> TargetType.FP16Target2
                    3 -> TargetType.FP16Target3
                    else -> TargetType.FP16Target4
                }
            }
            BufferQuality.HIGH_32 -> {
                when (channels) {
                    1 -> TargetType.FloatTarget1
                    2 -> TargetType.FloatTarget2
                    3 -> TargetType.FloatTarget3
                    else -> TargetType.FloatTarget4
                }
            }
        }
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
            cache.forEach { _, v ->
                val data = v.data
                if (data is FBStackData) {
                    data.nextIndex = 0
                }
            }
        }
    }

}