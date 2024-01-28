package me.anno.gpu.deferred

import me.anno.gpu.GFX
import me.anno.gpu.shader.renderer.Renderer.Companion.colorRenderer
import me.anno.gpu.shader.renderer.SimpleRenderer

val DeferredRenderer by lazy {
    SimpleRenderer(
        "deferred", DeferredSettings(
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
                // total: 14/15
            ) + (if (GFX.supportsDepthTextures) emptyList() else listOf(DeferredLayerType.DEPTH))
        ),
        colorRenderer.getPixelPostProcessing(0)
    )
}