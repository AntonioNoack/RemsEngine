package me.anno.engine.pbr

import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.SimpleRenderer

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
    "deferred", DeferredSettingsV2(
        listOf(
            DeferredLayerType.COLOR, // 3
            DeferredLayerType.NORMAL, // 2
            DeferredLayerType.EMISSIVE, // 3
            // DeferredLayerType.TANGENT,
            DeferredLayerType.OCCLUSION, // 1
            DeferredLayerType.ROUGHNESS, // 1
            DeferredLayerType.METALLIC,// 1
            DeferredLayerType.SHEEN, // 1
            DeferredLayerType.TRANSLUCENCY, // 1
            DeferredLayerType.ANISOTROPIC, // 1
            // total: 14
        ), 1, true
    ),
    colorRenderer.getPostProcessing(0)
)
