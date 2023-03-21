package me.anno.engine.pbr

import me.anno.gpu.GFX
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.SimpleRenderer
import kotlin.math.min

// many lights with many shadow maps would
// - require many heavy updates
// - require many, many textures for many lights

// todo static / moving entities:
// todo static lights never need to be updated in the knn map

// todo on low-end devices, merge color and emissive (alpha 0 => only diffuse, > 0 => diffuse + alpha * emissive)
// todo on low-end devices, roughness/metallic could be reduced into single number, or extra attributes joined with just flags

// done easily switch between forward rendering (k nearest neighbor algorithm or only a few lights)
// done and deferred rendering (no mapping needed, more memory intensive, more lights supported)

object DeferredRenderer : SimpleRenderer(
    "deferred", DeferredSettingsV2(findLayers(), 1, true),
    colorRenderer.getPostProcessing()!!
)

object DeferredRendererMSAA : SimpleRenderer(
    "deferredMSAA", DeferredSettingsV2(findLayers(), min(GFX.maxSamples, 8), true),
    colorRenderer.getPostProcessing()!!
)

// todo while these should be matches to the program running them, the game should decide what it needs where
fun findLayers() = when (8 + GFX.maxColorAttachments) {
    // my Huawei H10 has 4, my RX 580 has 8
    // we should program in such a way, that it always works
    1 -> listOf(DeferredLayerType.COLOR_EMISSIVE)
    2 -> listOf(
        DeferredLayerType.COLOR_EMISSIVE, // 4
        DeferredLayerType.NORMAL, // 3
        DeferredLayerType.ROUGHNESS, // 1
        // total: 8
    )
    3 -> listOf(
        DeferredLayerType.COLOR_EMISSIVE, // 4
        DeferredLayerType.NORMAL, // 3
        DeferredLayerType.ROUGHNESS, // 1
        DeferredLayerType.METALLIC, // 1
        DeferredLayerType.SHEEN, // 1
        DeferredLayerType.TRANSLUCENCY, // 1
        DeferredLayerType.ANISOTROPIC, // 1
        // total: 12
    )
    else -> listOf(
        DeferredLayerType.COLOR, // 3
        DeferredLayerType.NORMAL, // 3
        DeferredLayerType.EMISSIVE, // 3
        // DeferredLayerType.TANGENT,
        DeferredLayerType.OCCLUSION, // 1
        DeferredLayerType.ROUGHNESS, // 1
        DeferredLayerType.METALLIC,// 1
        DeferredLayerType.SHEEN, // 1
        DeferredLayerType.TRANSLUCENCY, // 1
        DeferredLayerType.ANISOTROPIC, // 1
        // total: 15
    )
}