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

    @Range(0.0, 1e10)
    var radius = 2f // 0.1 of world size looks pretty good :)

    // why is this soo expensive on my RTX3070?
    // memory limited...

    @Range(0.0, 4096.0)
    var samples = max(1, DefaultConfig["gpu.ssao.samples", 128])

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
                layers[DeferredLayerType.POSITION]!!.getTexture0(),
                layers[DeferredLayerType.NORMAL]!!.getTexture0(),
                RenderState.cameraMatrix,
                radius, strength, samples
            )
        )
    }

    override fun clone(): SSAOEffect {
        val clone = SSAOEffect()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SSAOEffect
        clone.samples = samples
        clone.radius = radius
    }

    override val className = "SSAOEffect"

    companion object {
        private val inputList = listOf(DeferredLayerType.POSITION, DeferredLayerType.NORMAL)
        private val outputList = listOf(DeferredLayerType.OCCLUSION)
    }

}