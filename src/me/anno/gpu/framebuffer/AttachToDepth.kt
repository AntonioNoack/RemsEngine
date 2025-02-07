package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.texture.Texture2D

object AttachToDepth {
    fun Framebuffer.attachFramebufferToDepthImpl(name: String, targets: List<TargetType>): IFramebuffer {
        if (depthBufferType == DepthBufferType.NONE)
            throw IllegalStateException("Cannot attach depth to framebuffer without depth buffer")
        return if (targets.size <= GFX.maxColorAttachments) {
            val buffer = Framebuffer(name, width, height, samples, targets, DepthBufferType.ATTACHMENT)
            buffer.depthAttachment = this
            buffer.ssBuffer?.depthAttachment = ssBuffer
            buffer
        } else {
            val buffer = MultiFramebuffer(name, width, height, samples, targets, DepthBufferType.ATTACHMENT)
            for (it in buffer.targetsI) {
                it.depthAttachment = this
                it.ssBuffer?.depthAttachment = ssBuffer
            }
            buffer
        }
    }

    fun Texture2D.attachFramebufferToDepthImpl(name: String, targets: List<TargetType>): IFramebuffer? {
        return owner?.attachFramebufferToDepthImpl(name, targets)
    }
}