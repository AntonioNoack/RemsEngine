package me.anno.gpu.deferred

import me.anno.gpu.GFX
import me.anno.gpu.shader.renderer.Renderer.Companion.colorRenderer
import me.anno.gpu.shader.renderer.SimpleRenderer

val DeferredRenderer by lazy { // lazy for GFX.supportsDepthTextures
    SimpleRenderer(
        "deferred", DeferredSettings(
            listOf(
                DeferredLayerType.COLOR, // 3
                DeferredLayerType.NORMAL, // 2
                DeferredLayerType.EMISSIVE, // 3
                DeferredLayerType.OCCLUSION, // 1 - remove?
                DeferredLayerType.REFLECTIVITY,// 1
                DeferredLayerType.SHEEN, // 1
                DeferredLayerType.TRANSLUCENCY, // 1 - merge with sheen?
                DeferredLayerType.ANISOTROPIC, // 1 - remove?
                // total: 13/15
            ) + (if (GFX.supportsDepthTextures) emptyList() else listOf(DeferredLayerType.DEPTH))
        ),
        colorRenderer.getPixelPostProcessing(0)
    )
}