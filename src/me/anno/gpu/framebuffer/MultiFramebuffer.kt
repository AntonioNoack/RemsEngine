package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.maths.Maths
import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min

/**
 * there is a target limit, so
 *  - bundle targets up to the limit,
 *  - share the depth
 *  - allow to bind all target types
 *  - when rendering, draw as often as required
 * */
class MultiFramebuffer(
    override val name: String,
    w: Int, h: Int,
    samples: Int, targets: Array<TargetType>,
    depthBufferType: DepthBufferType
) : IFramebuffer {

    constructor(
        name: String, w: Int, h: Int, samples: Int,
        targetCount: Int,
        fpTargets: Boolean,
        depthBufferType: DepthBufferType
    ) : this(
        name, w, h, samples, if (fpTargets)
            Array(targetCount) { TargetType.FloatTarget4 } else
            Array(targetCount) { TargetType.UByteTarget4 }, depthBufferType
    )

    val targetsI: Array<Framebuffer>
    val div = max(1, GFX.maxColorAttachments)

    init {
        val targetCount = ceilDiv(targets.size, div)
        targetsI = Array(targetCount) { targetIndex ->
            val targetIndex0 = targetIndex * div
            val targetIndex1 = min(targetIndex0 + div, targets.size)
            Framebuffer(
                "$name/$targetIndex", w, h, samples,
                Array(targetIndex1 - targetIndex0) { targets[targetIndex0 + it] },
                if (targetIndex == 0) depthBufferType
                else when (depthBufferType) {
                    DepthBufferType.NONE -> DepthBufferType.NONE
                    else -> DepthBufferType.ATTACHMENT
                }
            )
        }
        val firstBuffer = targetsI[0]
        for (i in 1 until targetCount) {
            targetsI[i].depthAttachment = firstBuffer
        }
    }

    override val numTextures: Int = targets.size

    override val w: Int get() = targetsI[0].w
    override val h: Int get() = targetsI[0].h

    override val pointer: Int
        get() = throw RuntimeException("Cannot bind directly")

    override val samples: Int = Maths.clamp(samples, 1, GFX.maxSamples)

    override fun checkSession() {
        for (target in targetsI) {
            target.checkSession()
        }
    }

    override fun attachFramebufferToDepth(targetCount: Int, fpTargets: Boolean): IFramebuffer {
        return targetsI[0].attachFramebufferToDepth(targetCount, fpTargets)
    }

    override fun attachFramebufferToDepth(targets: Array<TargetType>): IFramebuffer {
        return targetsI[0].attachFramebufferToDepth(targets)
    }

    override fun getTextureI(index: Int): ITexture2D {
        return targetsI[index / div].getTextureI(index % div)
    }

    override fun bindTextureI(index: Int, offset: Int, nearest: GPUFiltering, clamping: Clamping) {
        getTextureI(index).bind(offset, nearest, clamping)
    }

    override fun bindTextures(offset: Int, nearest: GPUFiltering, clamping: Clamping) {
        var delta = offset
        for (target in targetsI) {
            target.bindTextures(delta, nearest, clamping)
            delta += div
        }
    }

    override fun ensure() {
        for (target in targetsI) {
            target.ensure()
        }
    }

    override fun bindDirectly() {
        throw RuntimeException("Cannot bind directly")
    }

    override fun bindDirectly(w: Int, h: Int) {
        throw RuntimeException("Cannot bind directly")
    }

    override fun destroy() {
        for (target in targetsI) {
            target.destroy()
        }
    }

    override val depthTexture get() = targetsI[0].depthTexture

}