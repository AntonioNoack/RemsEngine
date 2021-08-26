package me.anno.engine.pbr

import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.ShaderPlus

// many lights with many shadow maps would
// - require many heavy updates
// - require many, many textures for many lights

// todo light data:
//  - light color
//  - light transform, probably best inverse
//  - falloff / type
//  - light curve texture (index?)
//  - shadow map 2D (plane) / 3D (cubemap)

// todo static / moving entities:
// todo static lights never need to be updated in the knn map

// todo easily switch between forward rendering (k nearest neighbor algorithm or only a few lights)
// todo and deferred rendering (no mapping needed, more memory intensive, more lights supported)

object DeferredRenderer : Renderer(
    "deferred", false,
    ShaderPlus.DrawMode.COLOR,
    DeferredSettingsV2(
        listOf(
            DeferredLayerType.POSITION,
            DeferredLayerType.NORMAL,
            // DeferredLayerType.TANGENT,
            DeferredLayerType.COLOR,
            DeferredLayerType.EMISSIVE,
            DeferredLayerType.ROUGHNESS,
            DeferredLayerType.METALLIC,
            DeferredLayerType.OCCLUSION,
            DeferredLayerType.CLEAR_COAT,
            DeferredLayerType.ANISOTROPIC
        ),
        true
    )
) {

    // todo we need a post processing shader

}