package me.anno.gpu.deferred

import me.anno.gpu.GFX
import me.anno.gpu.shader.renderer.Renderer.Companion.colorRenderer
import me.anno.gpu.shader.renderer.SimpleRenderer
import me.anno.utils.structures.lists.Lists.iff

@Deprecated("This is only used in debug RenderModes. Actually used buffers are determined by the currently used RenderGraph")
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
            ) + listOf(DeferredLayerType.DEPTH).iff(!GFX.supportsDepthTextures)
        ),
        colorRenderer.getPixelPostProcessing(0)
    )
}