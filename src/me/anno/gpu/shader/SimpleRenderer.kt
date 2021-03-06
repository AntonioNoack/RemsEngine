package me.anno.gpu.shader

import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.builder.ShaderStage

open class SimpleRenderer(
    name: String,
    isFakeColor: Boolean,
    drawMode: ShaderPlus.DrawMode,
    // null, if not deferred
    deferredSettings: DeferredSettingsV2?,
    private val postProcessingImpl: ShaderStage
) : Renderer(name, isFakeColor, drawMode, deferredSettings) {

    constructor(name: String, isFakeColor: Boolean, drawMode: ShaderPlus.DrawMode, postProcessingImpl: ShaderStage) :
            this(name, isFakeColor, drawMode, null, postProcessingImpl)

    override fun getPostProcessing(): ShaderStage? = postProcessingImpl
}