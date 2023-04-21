package me.anno.ecs.components.camera.effects

import me.anno.config.DefaultConfig
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.shaders.effects.ScreenSpaceAmbientOcclusion
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.IFramebuffer
import kotlin.math.max

class SSAOEffect : CameraEffect() {

    // todo this can become extremely with complex geometry
    // (40 fps on a RTX 3070 🤯, where a pure-color scene has 600 fps)
    // todo why is pure color soo slow? 600 fps instead of 1200 fps in mode "without post-processing")

    @Range(0.0, 1e10)
    var radius = 2f // 0.1 of world size looks pretty good :)

    // why is this soo expensive on my RTX3070?
    // memory limited...

    @Range(0.0, 4096.0)
    var samples = max(1, DefaultConfig["gpu.ssao.samples", 128])

    var enable2x2Blur = true

    override fun listInputs() = inputList
    override fun listOutputs() = outputList

    override fun render(
        buffer: IFramebuffer,
        format: DeferredSettingsV2,
        layers: MutableMap<DeferredLayerType, IFramebuffer>
    ) {
        write(
            layers, DeferredLayerType.OCCLUSION,
            ScreenSpaceAmbientOcclusion.compute(
                buffer.depthTexture!!,
                layers[DeferredLayerType.NORMAL]!!.getTexture0(),
                format.zw(DeferredLayerType.NORMAL),
                RenderState.cameraMatrix,
                radius, strength, samples,
                enable2x2Blur
            )
        )
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SSAOEffect
        dst.samples = samples
        dst.radius = radius
        dst.enable2x2Blur = enable2x2Blur
    }

    override val className: String get() = "SSAOEffect"

    companion object {
        private val inputList = listOf(DeferredLayerType.NORMAL)
        private val outputList = listOf(DeferredLayerType.OCCLUSION)
    }

}