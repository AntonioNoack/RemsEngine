package me.anno.graph.visual.render.effects

import me.anno.cache.ICacheData
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.graph.visual.render.effects.FrameGenInitNode.Companion.frameIndex
import me.anno.graph.visual.render.effects.FrameGenInitNode.Companion.totalFrames
import me.anno.graph.visual.render.effects.FrameGenInitNode.Companion.skipThisFrame
import me.anno.maths.Maths.posMod
import me.anno.utils.structures.maps.LazyMap

abstract class FrameGenOutputNode<PerViewData : ICacheData>(
    name: String,
    inputs: List<String>,
    outputs: List<String>
) : TimedRenderingNode(name, inputs, outputs) {

    abstract fun createPerViewData(): PerViewData
    abstract fun renderOriginal(view: PerViewData, width: Int, height: Int)
    abstract fun renderInterpolated(view: PerViewData, width: Int, height: Int, fraction: Float)

    val views = LazyMap { _: Int -> createPerViewData() }

    override fun executeAction() {
        timeRendering(name, timer) {
            val width = getIntInput(1)
            val height = getIntInput(2)
            val view = views[RenderState.viewIndex]
            if (skipThisFrame() && canInterpolate(view)) {
                val interFrames = totalFrames
                val frameIndex = posMod(frameIndex, interFrames)
                val fraction = frameIndex.toFloat() / interFrames.toFloat()
                renderInterpolated(view, width, height, fraction)
            } else {
                renderOriginal(view, width, height)
            }
        }
    }

    fun showOutput(data0: ITexture2D) {
        setOutput(1, Texture(data0, null, "xyz", DeferredLayerType.COLOR))
    }

    abstract fun canInterpolate(view: PerViewData): Boolean

    fun fill(width: Int, height: Int, data0: Texture2D, srcI: Int, targetType: TargetType, defaultTex: ITexture2D) {
        data0.resize(width, height, targetType)
        useFrame(data0) {
            val srcTex = (getInput(srcI) as? Texture).texOrNull ?: defaultTex
            GFX.copyNoAlpha(srcTex)
        }
    }

    override fun destroy() {
        super.destroy()
        for (view in views.values) {
            view.destroy()
        }
    }
}