package me.anno.gpu.texture

import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.TargetType

class IndestructibleTexture2DArray(
    name: String, w: Int, h: Int, d: Int,
    private val creationData: Any
) : Texture2DArray(name, w, h, d) {

    override fun destroy() {}

    private fun checkExistence() {
        checkSession()
        if (!wasCreated || isDestroyed) {
            isDestroyed = false
            when (creationData) {
                is ByteArray -> createRGBA(creationData)
                "depth" -> {
                    create(if (GFX.supportsDepthTextures) TargetType.DEPTH32F else TargetType.Float32x1)
                    depthFunc = DepthMode.CLOSER
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    override fun bind(index: Int, filtering: Filtering, clamping: Clamping): Boolean {
        checkExistence()
        return super.bind(index, filtering, clamping)
    }
}