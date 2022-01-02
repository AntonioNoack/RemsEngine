package me.anno.gpu.shader

import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.builder.ShaderStage

open class Renderer(

    val name: String,
    val isFakeColor: Boolean,
    val drawMode: ShaderPlus.DrawMode,
    // null, if not deferred
    val deferredSettings: DeferredSettingsV2? = null

) {

    open fun getPostProcessing(): ShaderStage? = null

    open fun uploadDefaultUniforms(shader: Shader) {}

    override fun toString(): String = name

    companion object {
        val colorRenderer = Renderer("color", false, ShaderPlus.DrawMode.COLOR, null)
        val colorSqRenderer = Renderer("colorSq", false, ShaderPlus.DrawMode.COLOR_SQUARED, null)
        val idRenderer = Renderer("id", true, ShaderPlus.DrawMode.ID, null)
        val depthRenderer01 = Renderer("depth01", true, ShaderPlus.DrawMode.DEPTH_LOG2_01, null)
        val depthRenderer = Renderer("depth", true, ShaderPlus.DrawMode.DEPTH_LOG2, null)
        val copyRenderer = Renderer("copy", false, ShaderPlus.DrawMode.COPY, null)
        val depthOnlyRenderer = Renderer("", true, ShaderPlus.DrawMode.DEPTH_LOG2, null)
    }

}