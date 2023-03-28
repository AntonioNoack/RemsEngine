package me.anno.ecs.components.camera.effects

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.shaders.effects.Bloom
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.IFramebuffer

class BloomEffect : ToneMappedEffect() {

    @Range(0.0, 1e10)
    var offset = 1f

    override fun render(
        buffer: IFramebuffer,
        format: DeferredSettingsV2,
        layers: MutableMap<DeferredLayerType, IFramebuffer>
    ) {
        val hdrInput = layers[DeferredLayerType.HDR_RESULT]!!.getTexture0()
        val output = Bloom.bloom2(hdrInput, offset, strength, applyToneMapping)
        write(layers, dstType, output)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as BloomEffect
        dst.offset = offset
    }

    override val className get() = "BloomEffect"

}