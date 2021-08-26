package me.anno.mesh.gltf

import de.javagl.jgltf.viewer.lwjgl.GlContextLwjgl
import me.anno.gpu.GFX
import me.anno.gpu.GFX.shaderColor
import me.anno.gpu.ShaderLib.positionPostProcessing
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderPlus
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import org.apache.logging.log4j.LogManager
import org.joml.Vector4f
import org.lwjgl.opengl.GL11.GL_NEAREST
import org.lwjgl.opengl.GL20
import java.nio.ByteBuffer

object CustomGlContext : GlContextLwjgl() {

    val tint = Vector4f()

    val shaders = ArrayList<Shader>()

    // val shaderCache = HashMap<String, Int>()
    lateinit var currentShader: Shader

    override fun createGlProgram(vertexShaderSource: String, fragmentShaderSource: String): Int {

        val index = shaders.size
        /*val key = vertexShaderSource + fragmentShaderSource
        val cache = shaderCache.getOrPut(key) { index }
        if (cache < index) {
            LOGGER.info("Reusing shader ${key.hashCode()}/$index")
            useGlProgram(index)
            return cache
        }*/

        var plusVertexShader = vertexShaderSource.trim()
        if (!plusVertexShader.endsWith("}")) throw RuntimeException()
        plusVertexShader = plusVertexShader.substring(0, plusVertexShader.length - 1) +
                positionPostProcessing +
                "}"
        val plusFragmentShader = ShaderPlus.makeFragmentShaderUniversal(emptyList(), fragmentShaderSource)
        val varying = listOf(Variable("float", "zDistance"))
        val shader = Shader("Gltf", null, plusVertexShader, varying, plusFragmentShader, true)
        shaders.add(shader)
        useGlProgram(index)
        return index

    }

    override fun useGlProgram(glProgram: Int) {
        val shader = shaders[glProgram]
        shader.use()
        shader.v1("drawMode", GFX.drawMode.id)
        shaderColor(shader, "tint", tint)
        currentShader = shader
    }

    override fun deleteGlProgram(glProgram: Int) {
        // LOGGER.info("deleting program $glProgram")
    }

    override fun getUniformLocation(glProgram: Int, uniformName: String): Int {
        return shaders[glProgram][uniformName]
    }

    override fun getAttributeLocation(glProgram: Int, attributeName: String): Int {
        return shaders[glProgram].getAttributeLocation(attributeName)
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

    override fun enable(states: MutableIterable<Number>) {
        // super.enable(states)
        // LOGGER.info("enabling ${states.map { getStateName(it as Int) }}")
    }

    override fun disable(states: MutableIterable<Number>) {
        // super.disable(states)
        // LOGGER.info("disabling ${states.map { getStateName(it as Int) }}")
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

    val textures = ArrayList<Texture2D>()
    override fun setUniformSampler(location: Int, textureIndex: Int, glTexture: Int) {
        val tex = textures[glTexture]
        tex.bind(textureIndex, tex.filtering, tex.clamping!!)
        GL20.glUniform1i(location, textureIndex)
    }

    override fun createGlTexture(
        pixelData: ByteBuffer,
        internalFormat: Int,
        width: Int,
        height: Int,
        format: Int,
        type: Int
    ): Int {
        val index = textures.size
        val tex = Texture2D("jGLTF", width, height, 1)
        tex.createRGBA(pixelData, true) // internal format & format always will be GL_RGBA
        textures.add(tex)
        return index
    }

    override fun deleteGlTexture(glTexture: Int) {
        // should never be called
        textures[glTexture].destroy()
    }

    override fun setGlTextureParameters(glTexture: Int, minFilter: Int, magFilter: Int, wrapS: Int, wrapT: Int) {

        /*
        * int minFilter = something or else GltfConstants.GL_NEAREST_MIPMAP_LINEAR
        * int magFilter = something or else GltfConstants.GL_LINEAR
        * */

        val tex = textures[glTexture]
        if (magFilter == GL_NEAREST) {
            tex.filtering = GPUFiltering.NEAREST
        }

        if (wrapS != wrapT) LOGGER.warn("Asymmetric wrapping not supported: $wrapS/$wrapT")
        for (clamping in Clamping.values()) {
            if (clamping.mode == wrapS || clamping.mode == wrapT) {
                tex.clamping = clamping
                break
            }
        }

    }

    private val LOGGER = LogManager.getLogger(CustomGlContext::class)


}