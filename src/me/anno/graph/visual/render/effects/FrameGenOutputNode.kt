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
import me.anno.graph.visual.render.effects.FrameGenInitNode.Companion.interFrames
import me.anno.utils.structures.maps.LazyMap

abstract class FrameGenOutputNode<PerViewDataI : FrameGenOutputNode.PerViewData>(
    name: String,
    inputs: List<String>,
    outputs: List<String>
) : TimedRenderingNode(name, inputs, outputs) {

    abstract class PerViewData : ICacheData {
        var frameIndex = Int.MAX_VALUE
    }

    abstract fun createPerViewData(): PerViewDataI
    abstract fun renderOriginal(view: PerViewDataI, width: Int, height: Int)
    abstract fun renderInterpolated(view: PerViewDataI, width: Int, height: Int)

    val views = LazyMap { _: Int -> createPerViewData() }

    override fun executeAction() {
        timeRendering(name, timer) {
            val width = getIntInput(1)
            val height = getIntInput(2)
            val view = views[RenderState.viewIndex]
            if (view.frameIndex < interFrames) {
                renderInterpolated(view, width, height)
                view.frameIndex++
            } else {
                renderOriginal(view, width, height)
                view.frameIndex = 0
            }
        }
    }

    fun showOutput(data0: ITexture2D) {
        setOutput(1, Texture(data0, null, "xyz", DeferredLayerType.COLOR))
    }

    fun fill(width: Int, height: Int, data0: Texture2D, srcI: Int, targetType: TargetType, defaultTex: ITexture2D) {
        data0.resize(width, height, targetType)
        useFrame(data0) {
            val srcTex = (getInput(srcI) as? Texture)?.texOrNull ?: defaultTex
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