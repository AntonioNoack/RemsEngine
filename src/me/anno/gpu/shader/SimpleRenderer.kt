package me.anno.gpu.shader

import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.builder.ShaderStage

open class SimpleRenderer(
    name: String,
    // null, if not deferred
    deferredSettings: DeferredSettingsV2?,
    private val postProcessingImpl: ShaderStage
) : Renderer(name, deferredSettings) {

    constructor(name: String, postProcessingImpl: ShaderStage) :
            this(name, null, postProcessingImpl)

    override fun getPostProcessing(flags: Int): ShaderStage? = postProcessingImpl
}