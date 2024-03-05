package me.anno.gpu.texture

import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.TargetType

class IndestructibleTexture2D(
    name: String, w: Int, h: Int,
    private val creationData: Any
) : Texture2D(name, w, h, 1) {

    override fun destroy() {}

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
                is IntArray -> createBGRA(
                    IntArray(creationData.size) { creationData[it] },
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
}