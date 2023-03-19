package me.anno.ecs.components.camera.effects

import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.shaders.effects.ScreenSpaceReflections
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.utils.Color.white4

class SSREffect : ToneMappedEffect() {

    @Range(0.0, 1e3)
    var maskSharpness = 1f

    @Range(0.0, 1e38)
    var wallThickness = 0.2f

    @Range(0.0, 1e38)
    var maxDistance = 8f

    @Docs("10 are enough, if there are only rough surfaces")
    @Range(1.0, 512.0)
    var fineSteps = 10

    override fun listInputs() = inputs

    override fun render(
        buffer: IFramebuffer,
        format: DeferredSettingsV2,
        layers: MutableMap<DeferredLayerType, IFramebuffer>
    ) {
        write(
            layers, dstType,
            ScreenSpaceReflections.compute(
                buffer.depthTexture!!,
                layers[DeferredLayerType.NORMAL]!!.getTexture0(),
                layers[DeferredLayerType.COLOR]!!.getTexture0(),
                layers[DeferredLayerType.EMISSIVE]!!.getTexture0(),
                layers[DeferredLayerType.METALLIC]!!.getTexture0(),
                format.findMapping(DeferredLayerType.METALLIC)!!,
                layers[DeferredLayerType.ROUGHNESS]!!.getTexture0(),
                format.findMapping(DeferredLayerType.ROUGHNESS)!!,
                layers[DeferredLayerType.HDR_RESULT]!!.getTexture0(),
                RenderState.cameraMatrix, RenderView.currentInstance?.pipeline?.skyBox, white4,
                strength,
                maskSharpness,
                wallThickness,
                fineSteps,
                maxDistance,
                applyToneMapping
            )
        )
    }

    override fun clone(): SSREffect {
        val clone = SSREffect()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SSREffect
        clone.maskSharpness = maskSharpness
        clone.wallThickness = wallThickness
        clone.maxDistance = maxDistance
        clone.fineSteps = fineSteps
    }

    override val className get() = "SSREffect"

    companion object {
        val inputs = listOf(
            DeferredLayerType.POSITION, DeferredLayerType.NORMAL,
            DeferredLayerType.COLOR, DeferredLayerType.METALLIC, DeferredLayerType.ROUGHNESS
        )
    }

}