package me.anno.ecs.components.camera.effects

import me.anno.Engine
import me.anno.ecs.annotations.Range
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.io.base.BaseWriter

// todo each camera effect shall become a node in RenderGraph
// @Deprecated("Shall be replaced by RenderGraph")
abstract class CameraEffect : PrefabSaveable() {

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
        (fb as? Framebuffer)?.lastDraw = Engine.nanoTime
        layers[type] = fb
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as CameraEffect
        clone.strength = strength
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveSerializableProperties(writer)
    }

    override fun readSomething(name: String, value: Any?) {
        if (!readSerializableProperty(name, value)) {
            super.readSomething(name, value)
        }
    }

}