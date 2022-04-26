package me.anno.ecs.components.camera.effects

import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.texture.ITexture2D

abstract class ColorMapEffect : CameraEffect() {

    override fun render(
        buffer: IFramebuffer,
        format: DeferredSettingsV2,
        layers: MutableMap<DeferredLayerType, IFramebuffer>
    ) {
        val sdrInput = layers[DeferredLayerType.SDR_RESULT]!!.getTexture0()
        val output = render(sdrInput)
        write(layers, DeferredLayerType.SDR_RESULT, output)
    }

    abstract fun render(color: ITexture2D): IFramebuffer

    override fun listInputs() = ioTypes
    override fun listOutputs() = ioTypes

    companion object {
        private val ioTypes = listOf(DeferredLayerType.SDR_RESULT)
    }
}