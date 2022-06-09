package me.anno.gpu.shader

import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.shader.builder.ShaderStage

open class Renderer(
    val name: String,
    val isFakeColor: Boolean,
    val drawMode: ShaderPlus.DrawMode,
    val deferredSettings: DeferredSettingsV2? = null // null, if not deferred
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
        val depthRenderer = Renderer("depth", true, ShaderPlus.DrawMode.DEPTH_DSQ, null)
        val copyRenderer = Renderer("copy", false, ShaderPlus.DrawMode.COPY, null)
        val triangleVisRenderer = Renderer("randomId", true, ShaderPlus.DrawMode.RANDOM_ID, null)
        val motionVectorRenderer = Renderer("motionVector", true, ShaderPlus.DrawMode.MOTION_VECTOR, null)

    }

}