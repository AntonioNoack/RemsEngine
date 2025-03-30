package me.anno.gpu.framebuffer

import me.anno.cache.ICacheData
import me.anno.cache.SimpleCache
import me.anno.gpu.GFX
import me.anno.gpu.deferred.BufferQuality
import me.anno.maths.Maths.clamp
import org.apache.logging.log4j.LogManager

object FBStack : SimpleCache("FBStack", 2100L) {

    private val LOGGER = LogManager.getLogger(FBStack::class)

    private abstract class FBStackData(
        val width: Int,
        val height: Int,
        private val samples: Int,
        targetTypes: List<TargetType>,
        depthBufferType: DepthBufferType
    ) : ICacheData {

        val readDepth = depthBufferType.read == true
        val writeDepth = depthBufferType.write

        var nextIndex = 0
        val data = ArrayList<IFramebuffer>()

        val hasExtraDepthBuffer get() = readDepth && !GFX.supportsDepthTextures
        val targetTypes = if (hasExtraDepthBuffer) {
            targetTypes + TargetType.Float32x1
        } else targetTypes

        override fun destroy() {
            if (data.isNotEmpty()) {
                for (it in data) {
                    it.destroy()
                }
                printDestroyed(data.size)
                data.clear()
            }
        }

        fun getFrame(name: String): IFramebuffer {
            val result = if (nextIndex >= data.size) {
                val depthBufferType = if (writeDepth) {
                    if (GFX.supportsDepthTextures) DepthBufferType.TEXTURE
                    else DepthBufferType.INTERNAL
                } else DepthBufferType.NONE
                val framebuffer = IFramebuffer.createFramebuffer(
                    name, width, height,
                    samples, targetTypes,
                    depthBufferType
                )
                data.add(framebuffer)
                if (hasExtraDepthBuffer) {
                    framebuffer.ensure() // ensure textures
                    // link depth texture to make things easier
                    when (framebuffer) {
                        is MultiFramebuffer -> {
                            framebuffer.depthTexture = framebuffer.targetsI.last().textures!!.last()
                            framebuffer.depthMask = 0
                        }
                        is Framebuffer -> {
                            framebuffer.depthTexture = framebuffer.textures!!.last()
                            framebuffer.depthMask = 0
                        }
                    }
                }
                nextIndex = data.size
                data.last()
            } else {
                val framebuffer = data[nextIndex++]
                framebuffer.name = name
                framebuffer
            }
            // reset isSRGB-mask
            result.isSRGBMask = 0
            return result
        }

        abstract fun printDestroyed(size: Int)
    }

    private data class FBKey1(
        val width: Int,
        val height: Int,
        val channels: Int,
        val quality: BufferQuality,
        val samples: Int,
        val depthBufferType: DepthBufferType
    )

    private class FBStackData1(val key: FBKey1) :
        FBStackData(
            key.width, key.height, key.samples,
            if (key.channels < 1) emptyList()
            else listOf(getTargetType(key.channels, key.quality)),
            key.depthBufferType
        ) {
        override fun printDestroyed(size: Int) {
            val fs = if (size == 1) "1 framebuffer" else "$size framebuffers"
            LOGGER.info("Freed $fs of size ${key.width} x ${key.height}, samples: ${key.samples}, quality: ${key.quality}")
        }
    }

    private data class FBKey2(
        val width: Int, val height: Int, val targetType: TargetType,
        val samples: Int, val depthBufferType: DepthBufferType
    )

    private data class FBKey3(
        val width: Int, val height: Int, val targetTypes: List<TargetType>,
        val samples: Int, val depthBufferType: DepthBufferType
    )

    private class FBStackData2(val key: FBKey2) :
        FBStackData(key.width, key.height, key.samples, listOf(key.targetType), key.depthBufferType) {
        override fun printDestroyed(size: Int) {
            val fs = if (size == 1) "1 framebuffer" else "$size framebuffers"
            LOGGER.info("Freed $fs of size ${key.width} x ${key.height}, samples: ${key.samples}, type: ${key.targetType}")
        }
    }

    private class FBStackData3(val key: FBKey3) :
        FBStackData(key.width, key.height, key.samples, key.targetTypes, key.depthBufferType) {
        override fun printDestroyed(size: Int) {
            val fs = if (size == 1) "1 framebuffer" else "$size framebuffers"
            LOGGER.info("Freed $fs of size ${key.width} x ${key.height}, samples: ${key.samples}, type: ${key.targetTypes}")
        }
    }

    private fun getValue(
        width: Int, height: Int, channels: Int,
        quality: BufferQuality, samples: Int,
        depthBufferType: DepthBufferType
    ): FBStackData {
        val key = FBKey1(width, height, channels, quality, clamp(samples, 1, GFX.maxSamples), depthBufferType)
        return getEntry(key, ::FBStackData1)!!
    }

    private fun getValue(
        width: Int, height: Int, targetType: TargetType, samples: Int,
        depthBufferType: DepthBufferType
    ): FBStackData {
        val key = FBKey2(width, height, targetType, clamp(samples, 1, GFX.maxSamples), depthBufferType)
        return getEntry(key, ::FBStackData2)!!
    }

    private fun getValue(
        w: Int, h: Int, targetTypes: List<TargetType>, samples: Int,
        depthBufferType: DepthBufferType
    ): FBStackData {
        val key = FBKey3(w, h, targetTypes, clamp(samples, 1, GFX.maxSamples), depthBufferType)
        return getEntry(key, ::FBStackData3)!!
    }

    operator fun get(
        name: String, w: Int, h: Int, channels: Int,
        quality: BufferQuality, samples: Int,
        depthBufferType: DepthBufferType
    ): IFramebuffer {
        val value = getValue(w, h, channels, quality, samples, depthBufferType)
        synchronized(value) {
            return value.getFrame(name)
        }
    }

    operator fun get(
        name: String, w: Int, h: Int, channels: Int,
        fp: Boolean, samples: Int, depthBufferType: DepthBufferType
    ): IFramebuffer {
        val quality = if (fp) BufferQuality.FP_32 else BufferQuality.UINT_8
        return get(name, w, h, channels, quality, samples, depthBufferType)
    }

    operator fun get(
        name: String, w: Int, h: Int,
        targetType: TargetType, samples: Int,
        depthBufferType: DepthBufferType
    ): IFramebuffer {
        val value = getValue(w, h, targetType, samples, depthBufferType)
        synchronized(value) {
            return value.getFrame(name)
        }
    }

    operator fun get(
        name: String, w: Int, h: Int,
        targetTypes: List<TargetType>, samples: Int,
        depthBufferType: DepthBufferType
    ): IFramebuffer {
        val value = getValue(w, h, targetTypes, samples, depthBufferType)
        synchronized(value) {
            return value.getFrame(name)
        }
    }

    fun getTargetType(channels: Int, quality: BufferQuality): TargetType {
        return when (quality) {
            BufferQuality.UINT_8 -> TargetType.UInt8xI
            BufferQuality.UINT_16 -> TargetType.UInt16xI
            BufferQuality.FP_16 -> TargetType.Float16xI
            BufferQuality.DEPTH_U32, BufferQuality.FP_32 -> TargetType.Float32xI
        }[channels - 1]
    }

    fun reset(w: Int, h: Int) {
        synchronized(cache) {
            for (value in cache.values) {
                val data = value.data
                if (data is FBStackData && data.width == w && data.height == h) {
                    data.nextIndex = 0
                }
            }
        }
    }

    fun reset() {
        resetFBStack()
    }

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