package me.anno.engine.pbr

import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.ShaderPlus

// many lights with many shadow maps would
// - require many heavy updates
// - require many, many textures for many lights

// todo static / moving entities:
// todo static lights never need to be updated in the knn map

// done easily switch between forward rendering (k nearest neighbor algorithm or only a few lights)
// done and deferred rendering (no mapping needed, more memory intensive, more lights supported)

object DeferredRenderer : Renderer(
    "deferred", false,
    ShaderPlus.DrawMode.COLOR,
    DeferredSettingsV2(
        listOf(
            DeferredLayerType.COLOR, // 3
            DeferredLayerType.OCCLUSION, // 1
            DeferredLayerType.NORMAL, // 3
            DeferredLayerType.POSITION, // 3, could be replaced by depth + transform + math
            DeferredLayerType.EMISSIVE, // 3
            // DeferredLayerType.TANGENT,
            DeferredLayerType.ROUGHNESS, // 1
            DeferredLayerType.METALLIC,// 1
            DeferredLayerType.SHEEN, // 1
            DeferredLayerType.TRANSLUCENCY, // 1
            // applied in material shader
            // DeferredLayerType.CLEAR_COAT,
            DeferredLayerType.ANISOTROPIC // 1
        ),
        true
    )
)