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

    fun split(index: Int, spliceSize: Int): Renderer {
        if (deferredSettings == null) return this
        return cache!!.getOrPut(index.shl(16) + spliceSize) {
            val settings = deferredSettings.split(index, spliceSize)
            object : Renderer("$name/$index/$spliceSize", isFakeColor, drawMode, settings) {
                override fun getPostProcessing(): ShaderStage? = this@Renderer.getPostProcessing()
                override fun uploadDefaultUniforms(shader: Shader) {
                    this@Renderer.getPostProcessing()
                }
            }
        }
    }

    val cache = if (deferredSettings == null) null else HashMap<Int, Renderer>()

    override fun toString(): String = name

    companion object {
        val colorRenderer = Renderer("color", false, ShaderPlus.DrawMode.COLOR, null)
        val colorSqRenderer = Renderer("colorSq", false, ShaderPlus.DrawMode.COLOR_SQUARED, null)
        val idRenderer = Renderer("id", true, ShaderPlus.DrawMode.ID, null)
        val idRendererVis = Renderer("idVis", true, ShaderPlus.DrawMode.ID, null)
        val depthRenderer01 = Renderer("depth01", true, ShaderPlus.DrawMode.DEPTH_LOG2_01, null)
        val depthRenderer = Renderer("depth", true, ShaderPlus.DrawMode.DEPTH_LOG2, null)
        val copyRenderer = Renderer("copy", false, ShaderPlus.DrawMode.COPY, null)
        val depthOnlyRenderer = Renderer("depthOnly", true, ShaderPlus.DrawMode.DEPTH_LOG2, null)
        val randomIdRenderer = Renderer("randomId", true, ShaderPlus.DrawMode.RANDOM_ID, null)
    }

}