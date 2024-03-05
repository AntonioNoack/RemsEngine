package me.anno.gpu.texture

import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.TargetType

class IndestructibleCubemap(
    name: String, w: Int,
    private val creationData: Any
) : CubemapTexture(name, w, 1) {

    override fun destroy() {}

    private fun checkExistence() {
        checkSession()
        if (!wasCreated || isDestroyed) {
            isDestroyed = false
            when (creationData) {
                is ByteArray -> createRGBA(Array(6) { creationData }.toList())
                "depth" -> {
                    create(if (GFX.supportsDepthTextures) TargetType.DEPTH32F else TargetType.Float32x1)
                    depthFunc = DepthMode.CLOSER
                }
                else -> throw IllegalArgumentException("Unknown constructor data")
            }
        }
    }

    override fun bind(index: Int, filtering: Filtering, clamping: Clamping): Boolean {
        checkExistence()
        return super.bind(index, filtering, clamping)
    }

    override fun bindTrulyNearest(index: Int): Boolean {
        checkExistence()
        return super.bindTrulyNearest(index)
    }
}