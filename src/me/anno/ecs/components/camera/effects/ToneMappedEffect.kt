package me.anno.ecs.components.camera.effects

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.deferred.DeferredLayerType
import org.joml.Vector3f
import kotlin.math.max

abstract class ToneMappedEffect : CameraEffect() {

    var applyToneMapping = false
    val dstType get() = if (applyToneMapping) DeferredLayerType.SDR_RESULT else DeferredLayerType.HDR_RESULT

    override fun listOutputs() = if (applyToneMapping) sdrOutputs else hdrOutputs

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as ToneMappedEffect
        dst.applyToneMapping = applyToneMapping
    }

    companion object {
        private val sdrOutputs = listOf(DeferredLayerType.SDR_RESULT)
        private val hdrOutputs = listOf(DeferredLayerType.HDR_RESULT)
    }
}