package me.anno.gpu.shader.renderer

import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.shader.builder.ShaderStage

open class SimpleRenderer(
    name: String,
    // null, if not deferred
    deferredSettings: DeferredSettings?,
    private val postProcessing: List<ShaderStage>
) : Renderer(name, deferredSettings) {
    constructor(name: String, postProcessing: ShaderStage) : this(name, null, listOf(postProcessing))

    override fun getPixelPostProcessing(flags: Int): List<ShaderStage> = postProcessing
}