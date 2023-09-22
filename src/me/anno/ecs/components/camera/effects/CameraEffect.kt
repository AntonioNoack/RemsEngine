package me.anno.ecs.components.camera.effects

import me.anno.Time
import me.anno.ecs.annotations.Range
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.texture.ITexture2D

// todo each camera effect shall become a node in RenderGraph
@Deprecated("Shall be replaced by RenderGraph")
abstract class CameraEffect {

    @Range(0.0, 1e10)
    var strength = 1f

    // todo integrate this into RenderView
    // todo integrate all useful effects from Three.js
    // to do pixel-local shaders (ColorMapEffect): they could be stacked within one shader to reduce bandwidth requirements & rounding
    // (tone mapping, color correction, ...)

    open fun listInputs(): List<DeferredLayerType> = emptyList()
    open fun listOutputs(): List<DeferredLayerType> = emptyList()
    abstract fun render(
        buffer: IFramebuffer,
        format: DeferredSettingsV2,
        layers: MutableMap<DeferredLayerType, IFramebuffer>
    )

    fun write(layers: MutableMap<DeferredLayerType, IFramebuffer>, type: DeferredLayerType, fb: IFramebuffer) {
        (fb as? Framebuffer)?.lastDrawn = Time.gameTimeN
        layers[type] = fb
    }

    fun write(layers: MutableMap<DeferredLayerType, IFramebuffer>, type: DeferredLayerType, fb: ITexture2D) {
        layers[type] = fb.wrapAsFramebuffer()
    }
}