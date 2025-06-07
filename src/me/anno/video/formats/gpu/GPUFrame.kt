package me.anno.video.formats.gpu

import me.anno.cache.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.drawing.GFXx2D.tiling
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.utils.assertions.assertTrue
import me.anno.utils.pooling.Pools
import me.anno.utils.structures.maps.LazyMap
import org.apache.logging.log4j.LogManager
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * GPU frames are synthetic textures that need a special shader, and might take up multiple texture slots.
 * They are used to efficiently stream video from FFMPEG and similar to the GPU.
 * */
abstract class GPUFrame(val width: Int, val height: Int, val numChannels: Int) : ICacheData {

    init {
        assertTrue(width > 0)
        assertTrue(height > 0)
    }

    var frameIndex = -1

    val isCreated: Boolean get() = getTextures().all { it.wasCreated && !it.isDestroyed }
    val isDestroyed: Boolean get() = getTextures().any { it.isDestroyed }

    val blankDetector = BlankFrameDetector()

    fun isBlankFrame(f0: GPUFrame, f4: GPUFrame, outlierThreshold: Float = 1f): Boolean {
        return blankDetector.isBlankFrame(f0.blankDetector, f4.blankDetector, outlierThreshold)
    }

    fun get3DShaderPlanar(): BaseShader {
        return shaderMap3d[getShaderStage()]
    }

    fun get2DShader(): Shader {
        return shaderMap2d[getShaderStage()]
    }

    open fun getShaderStage(): ShaderStage = swizzleStage0

    abstract fun getTextures(): List<Texture2D>

    fun bind(offset: Int, nearestFiltering: Filtering, clamping: Clamping) {
        val tex = getTextures()
        for (i in tex.indices.reversed()) {
            tex[i].bind(offset + i, nearestFiltering, clamping)
        }
    }

    open fun getByteSize(): Long {
        return width.toLong() * height.toLong() * numChannels.toLong()
    }

    fun bind(offset: Int, filtering: Filtering, clamping: Clamping, tex: List<ITexture2D>) {
        for (index in tex.indices.reversed()) {
            tex[index].bind(offset + index, filtering, clamping)
        }
    }

    override fun destroy() {
        for (texture in getTextures()) {
            texture.destroy()
        }
    }

    fun interlace(a: ByteBuffer, b: ByteBuffer, dst: ByteBuffer): ByteBuffer {
        val size = a.limit()
        for (i in 0 until size) {
            dst.put(a[i])
            dst.put(b[i])
        }
        dst.flip()
        return dst
    }

    fun interlaceReplace(a: ByteBuffer, b: ByteBuffer): ByteBuffer {
        val pool = Pools.byteBufferPool
        val dst = pool[a.remaining() * 2, false, false]
        interlace(a, b, dst)
        pool.returnBuffer(a)
        pool.returnBuffer(b)
        return dst
    }

    abstract fun load(input: InputStream, callback: (GPUFrame?) -> Unit)

    open fun bindUVCorrection(shader: Shader) {
        val w = width
        val h = height
        shader.v2f("uvCorrection", w.toFloat() / ((w + 1) / 2 * 2), h.toFloat() / ((h + 1) / 2 * 2))
    }

    /**
     * Creates a new texture, which contains the image data of the frame
     * */
    fun toTexture(flipY: Boolean = false): Texture2D {
        return toTexture(Texture2D("GpuFrame", width, height, 1), flipY)
    }

    /**
     * Creates a new texture, which contains the image data of the frame
     * */
    fun toTexture(texture: Texture2D, flipY: Boolean = false): Texture2D {
        GFX.checkIsGFXThread()
        if (texture.pointer == 0) {
            texture.create(TargetType.UInt8xI[numChannels - 1])
            texture.channels = numChannels
        }
        GFXState.useFrame(texture) {
            GFXState.renderPurely {
                val shader = get2DShader()
                shader.use()
                posSize(shader, 0, 0, texture.width, texture.height, flipY)
                tiling(shader, GFXx2D.noTiling)
                bind(0, Filtering.TRULY_LINEAR, Clamping.CLAMP)
                bindUVCorrection(shader)
                SimpleBuffer.flat01.draw(shader)
                GFX.check()
            }
        }
        GFX.check()
        return texture
    }

    fun warnAlreadyDestroyed(data0: ByteBuffer?, data1: ByteBuffer?) {
        val pool = Pools.byteBufferPool
        pool.returnBuffer(data0)
        pool.returnBuffer(data1)
        LogManager.getLogger(this::class)
            .warn("Frame is already destroyed!")
    }

    companion object {

        @JvmField
        val swizzleStage0 = ShaderStage(
            "loadTex", listOf(
                Variable(GLSLType.V2F, "finalUV"),
                Variable(GLSLType.S2D, "tex"),
                Variable(GLSLType.V4F, "color", VariableMode.OUT)
            ), "color = getTexture(tex, finalUV);\n"
        )

        @JvmField
        val swizzleStageBGRA = ShaderStage(
            "loadTex", listOf(
                Variable(GLSLType.V2F, "finalUV"),
                Variable(GLSLType.S2D, "tex"),
                Variable(GLSLType.V4F, "color", VariableMode.OUT)
            ), "color = getTexture(tex, finalUV).bgra;\n"
        )

        @JvmField
        val swizzleStageARGB = ShaderStage(
            "loadTex", listOf(
                Variable(GLSLType.V2F, "finalUV"),
                Variable(GLSLType.S2D, "tex"),
                Variable(GLSLType.V4F, "color", VariableMode.OUT)
            ), "color = getTexture(tex, finalUV).argb;\n"
        )

        @JvmField
        val swizzleStageMono = ShaderStage(
            "loadTex", listOf(
                Variable(GLSLType.V2F, "finalUV"),
                Variable(GLSLType.S2D, "tex"),
                Variable(GLSLType.V4F, "color", VariableMode.OUT)
            ), "color = vec4(getTexture(tex, finalUV).xxx,1.0);\n"
        )

        private val shaderMap2d = LazyMap<ShaderStage, Shader> { stage ->
            Shader(
                "2d-GPUFrame",
                ShaderLib.uiVertexShaderList, ShaderLib.uiVertexShader,
                ShaderLib.uvList, stage.variables.filter { !it.isOutput } + listOf(
                    Variable(GLSLType.V4F, "color", VariableMode.OUT),
                ), "" +
                        "vec4 getTexture(sampler2D tex, vec2 uv) {\n" +
                        "   return texture(tex,uv);\n" +
                        "}\n" +
                        // duv ~ 1/textureSize, used in Rem's Studio
                        "vec4 getTexture(sampler2D tex, vec2 uv, vec2 duv) {\n" +
                        "   return texture(tex,uv);\n" +
                        "}\n" +
                        stage.functions.joinToString("\n") { it.body } +
                        "void main() {\n" +
                        "   vec2 finalUV = uv;\n" +
                        stage.body +
                        "}"
            )
        }

        private val shaderMap3d = LazyMap<ShaderStage, BaseShader> { key ->
            ShaderLib.createShader(
                "3d-${this::class.simpleName}", ShaderLib.v3Dl, ShaderLib.v3D, ShaderLib.y3D,
                key.variables.filter { !it.isOutput } + listOf(
                    Variable(GLSLType.V4F, "tiling"),
                    Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
                    Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
                ), "" +
                        ShaderLib.applyTiling +
                        "vec4 getTexture(sampler2D tex, vec2 uv) {\n" +
                        "   uv = applyTiling(uv, tiling);\n" +
                        "   return texture(tex, uv);\n" +
                        "}\n" +
                        // used by Rem's Studio
                        "vec4 getTexture(sampler2D tex, vec2 uv, vec2 duv) {\n" +
                        "   return getTexture(tex,uv);\n" +
                        "}\n" +
                        "void main() {\n" +
                        "   vec2 finalUV = uv;\n" +
                        "   vec4 color;\n" +
                        key.body +
                        "   finalColor = color.rgb;\n" +
                        "   finalAlpha = color.a;\n" +
                        "}", listOf("tex")
            )
        }
    }
}