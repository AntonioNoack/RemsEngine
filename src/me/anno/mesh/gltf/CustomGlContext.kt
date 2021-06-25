package me.anno.mesh.gltf

import de.javagl.jgltf.viewer.lwjgl.GlContextLwjgl
import me.anno.gpu.GFX
import me.anno.gpu.GFX.shaderColor
import me.anno.gpu.ShaderLib.positionPostProcessing
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderPlus
import org.joml.Vector4f

object CustomGlContext : GlContextLwjgl() {

    val tint = Vector4f()

    // todo hashmap<string, shader> to reuse shaders
    val shaders = ArrayList<Shader>()
    override fun createGlProgram(vertexShaderSource: String, fragmentShaderSource: String): Int {
        var plusVertexShader = vertexShaderSource.trim()
        if (!plusVertexShader.endsWith("}")) throw RuntimeException()
        plusVertexShader = plusVertexShader.substring(0, plusVertexShader.length - 1) +
                positionPostProcessing +
                "}"
        val plusFragmentShader = ShaderPlus.makeFragmentShaderUniversal("", fragmentShaderSource)
        val shader = Shader("Gltf", plusVertexShader, "varying float zDistance;\n", plusFragmentShader, true)
        val index = shaders.size
        shaders.add(shader)
        useGlProgram(index)
        return index
    }

    override fun useGlProgram(glProgram: Int) {
        val shader = shaders[glProgram]
        shader.use()
        shader.v1("drawMode", GFX.drawMode.id)
        shaderColor(shader, "tint", tint)
    }

    override fun deleteGlProgram(glProgram: Int) {
        // println("deleting program $glProgram")
    }

    override fun getUniformLocation(glProgram: Int, uniformName: String): Int {
        return shaders[glProgram][uniformName]
    }

    override fun getAttributeLocation(glProgram: Int, attributeName: String): Int {
        return shaders[glProgram].getAttributeLocation(attributeName)
    }

    override fun enable(states: MutableIterable<Number>) {
        // super.enable(states)
        // println("enabling ${states.map { getStateName(it as Int) }}")
    }

    override fun disable(states: MutableIterable<Number>) {
        // super.disable(states)
        // println("disabling ${states.map { getStateName(it as Int) }}")
    }

    /*fun getStateName(state: Int): String {
        return when (state) {
            GL_BLEND -> "blend"
            GL_CULL_FACE -> "cull face"
            GL_DEPTH_TEST -> "depth test"
            GL_POLYGON_OFFSET_FILL -> "polygon offset fill"
            GL_SAMPLE_ALPHA_TO_COVERAGE -> "sample alpha to coverage"
            GL_SCISSOR_TEST -> "scissor test"
            GL_FRONT -> "front"
            GL_BACK -> "back"
            GL_FRONT_AND_BACK -> "both"
            else -> "$state"
        }
    }*/

    override fun setCullFace(mode: Int) {
        // super.setCullFace(mode)
    }

    override fun setDepthRange(zNear: Float, zFar: Float) {
        // super.setDepthRange(zNear, zFar)
    }

    override fun setBlendColor(r: Float, g: Float, b: Float, a: Float) {
        // super.setBlendColor(r, g, b, a)
    }

    override fun setBlendEquationSeparate(modeRgb: Int, modeAlpha: Int) {
        // super.setBlendEquationSeparate(modeRgb, modeAlpha)
    }

    override fun setBlendFuncSeparate(srcRgb: Int, dstRgb: Int, srcAlpha: Int, dstAlpha: Int) {
        // super.setBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha)
    }

    override fun setColorMask(r: Boolean, g: Boolean, b: Boolean, a: Boolean) {
        // super.setColorMask(r, g, b, a)
    }

    override fun setDepthFunc(func: Int) {
        // super.setDepthFunc(func)
    }

    override fun setDepthMask(mask: Boolean) {
        // super.setDepthMask(mask)
    }

    override fun setFrontFace(mode: Int) {
        // super.setFrontFace(mode)
    }


}