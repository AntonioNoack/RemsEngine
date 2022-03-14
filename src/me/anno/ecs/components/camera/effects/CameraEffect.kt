package me.anno.ecs.components.camera.effects

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.annotations.Range
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer

// todo these effects don't necessarily need to be components, they could just be prefab saveables
abstract class CameraEffect : Component() {

    @Range(0.0, 1e10)
    var strength = 0f

    // todo integrate this into RenderView
    // todo integrate all useful effects from Three.js
    // todo pixel-local shaders: they could be stacked within one shader to reduce bandwidth requirements & rounding

    open fun listInputs(): List<DeferredLayerType> = emptyList()
    open fun listOutputs(): List<DeferredLayerType> = emptyList()
    open fun render(
        buffer: IFramebuffer,
        format: DeferredSettingsV2,
        layers: HashMap<DeferredLayerType, IFramebuffer>
    ) {
    }

    fun write(layers: HashMap<DeferredLayerType, IFramebuffer>, type: DeferredLayerType, fb: IFramebuffer) {
        (fb as? Framebuffer)?.lastDraw = Engine.nanoTime
        layers[type] = fb
    }

}