package me.anno.gpu.shader.renderer

import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderStage

open class InheritedRenderer(
    name: String,
    val renderer: Renderer
) : Renderer(name, renderer.deferredSettings) {
    override fun getVertexPostProcessing(flags: Int): List<ShaderStage> = renderer.getVertexPostProcessing(flags)
    override fun getPixelPostProcessing(flags: Int): List<ShaderStage> = renderer.getPixelPostProcessing(flags)
    override fun bind(shader: Shader) {
        renderer.bind(shader)
    }
}