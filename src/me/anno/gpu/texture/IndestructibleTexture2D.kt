package me.anno.gpu.texture

import me.anno.cache.CacheSection
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.TargetType
import org.joml.Vector4f

/**
 * Texture, that is to be kept alive for the whole runtime;
 * can be created on any thread; will renew itself automatically, if the session is invalidated/recreated
 * */
class IndestructibleTexture2D(
    name: String, w: Int, h: Int,
    private val creationData: Any,
    private val canDestroy: Boolean = false
) : Texture2D(name, w, h, 1) {

    override fun destroy() {
        if (canDestroy) super.destroy()
    }

    fun ensureExists() {
        checkSession()
        if (!wasCreated || isDestroyed) {
            isDestroyed = false
            when (creationData) {
                is ByteArray -> {
                    if (creationData.size == width * height) {
                        createMonochrome(creationData, false)
                    } else {
                        createRGBA(creationData, false)
                    }
                }
                is FloatArray -> createRGBA(creationData, false)
                is IntArray -> createBGRA(
                    creationData,
                    false
                )
                "depth" -> {
                    create(TargetType.DEPTH16)
                    depthFunc = if (GFX.supportsClipControl) DepthMode.CLOSER else DepthMode.FORWARD_CLOSER
                }
                else -> throw IllegalArgumentException("Unknown constructor data")
            }
        }
    }

    override fun bind(index: Int, filtering: Filtering, clamping: Clamping): Boolean {
        ensureExists()
        val hasSize = width > 1 || height > 1
        return super.bind(
            index, if (hasSize) filtering else this.filtering,
            if (hasSize) clamping else this.clamping
        )
    }

    companion object {
        private val cachedTextures = CacheSection("TexCache")
        fun getColorTexture(color: Vector4f): ITexture2D {
            return cachedTextures.getEntry(color, 1000L, false) { key ->
                IndestructibleTexture2D(
                    "texCacheColor", 1, 1,
                    floatArrayOf(key.x, key.y, key.z, key.w), true
                )
            } as ITexture2D
        }
    }
}