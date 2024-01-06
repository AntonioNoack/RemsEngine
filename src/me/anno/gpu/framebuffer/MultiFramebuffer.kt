package me.anno.gpu.framebuffer

import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
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
    name: String,
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
            Array(targetCount) { TargetType.Float32x4 } else
            Array(targetCount) { TargetType.UInt8x4 }, depthBufferType
    )

    val targetsI: Array<Framebuffer>
    val div = max(1, GFX.maxColorAttachments)

    override var name: String = name
        set(value) {
            field = value
            for (i in targetsI.indices) {
                targetsI[i].name = "$value/$i"
            }
        }

    init {
        val targetCount = ceilDiv(targets.size, div)
        targetsI = Array(targetCount) { targetIndex ->
            val targetIndex0 = targetIndex * div
            val targetIndex1 = min(targetIndex0 + div, targets.size)
            Framebuffer(
                "$name/$targetIndex", w, h, samples,
                Array(targetIndex1 - targetIndex0) { targets[targetIndex0 + it] },
                /*if (targetIndex == 0) depthBufferType
                else when (depthBufferType) {
                    DepthBufferType.NONE -> DepthBufferType.NONE
                    else -> DepthBufferType.ATTACHMENT
                }*/
                depthBufferType
            )
        }
        val firstBuffer = targetsI[0]
        for (i in 1 until targetCount) {
            targetsI[i].depthAttachment = firstBuffer
        }
    }

    override fun getTargetType(slot: Int) = targetsI[slot / div].getTargetType(slot % div)

    override val numTextures: Int = targets.size

    override val width: Int get() = targetsI[0].width
    override val height: Int get() = targetsI[0].height

    override val pointer: Int
        get() = throw RuntimeException("Cannot bind directly")

    override val samples: Int = Maths.clamp(samples, 1, GFX.maxSamples)

    override fun checkSession() {
        for (target in targetsI) {
            target.checkSession()
        }
    }

    override fun attachFramebufferToDepth(name: String, targets: Array<TargetType>): IFramebuffer {
        return targetsI[0].attachFramebufferToDepth(name, targets)
    }

    override fun getTextureI(index: Int): ITexture2D {
        return targetsI[index / div].getTextureI(index % div)
    }

    override fun bindTextureI(index: Int, offset: Int, nearest: Filtering, clamping: Clamping) {
        getTextureI(index).bind(offset, nearest, clamping)
    }

    override fun bindTextures(offset: Int, nearest: Filtering, clamping: Clamping) {
        var delta = offset
        for (target in targetsI) {
            target.bindTextures(delta, nearest, clamping)
            delta += div
        }
    }

    val withMultisampling get() = samples > 1

    override fun getTextureIMS(index: Int): ITexture2D {
        return if (withMultisampling) targetsI[index / div].getTextureIMS(index % div)
        else getTextureI(index)
    }

    override fun bindTrulyNearestMS(offset: Int) {
        if (withMultisampling) {
            var k = offset
            for (i in targetsI.indices) {
                val textures = targetsI[i].textures!!
                for (j in textures.indices) {
                    textures[j].bindTrulyNearest(k++)
                }
            }
        } else super.bindTrulyNearestMS(offset)
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

    override fun use(index: Int, renderer: Renderer, render: () -> Unit) {
        val targets = targetsI
        for (targetIndex in targets.indices) {
            val target = targets[targetIndex]
            // split renderer by targets
            GFXState.renderers[index] = renderer.split(targetIndex, div)
            GFXState.framebuffer.use(target, render)
        }
    }

    override fun isBound(): Boolean {
        val curr = GFXState.currentBuffer
        return curr == this || targetsI.any { it.isBound() }
    }

    override var depthTexture
        get() = targetsI[0].depthTexture
        set(value) {
            for (target in targetsI) {
                target.depthTexture = value
            }
        }
}