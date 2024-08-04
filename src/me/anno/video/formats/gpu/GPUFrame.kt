package me.anno.video.formats.gpu

import me.anno.cache.ICacheData
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.SimpleBuffer
import me.anno.gpu.drawing.GFXx2D.posSize
import me.anno.gpu.drawing.GFXx2D.tiling
import me.anno.gpu.framebuffer.IFramebuffer
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
import me.anno.image.raw.ByteImage
import me.anno.utils.OS.desktop
import me.anno.utils.assertions.assertTrue
import me.anno.utils.files.Files.findNextFile
import me.anno.utils.structures.maps.LazyMap
import org.apache.logging.log4j.LogManager
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.Semaphore

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
        for (i in tex.lastIndex downTo 0) {
            tex[i].bind(offset + i, nearestFiltering, clamping)
        }
    }

    open fun getByteSize(): Long {
        return width.toLong() * height.toLong() * numChannels.toLong()
    }

    fun bind(offset: Int, filtering: Filtering, clamping: Clamping, tex: List<ITexture2D>) {
        for ((index, texture) in tex.withIndex().reversed()) {
            texture.bind(offset + index, filtering, clamping)
        }
    }

    fun bind2(offset: Int, filtering: Filtering, clamping: Clamping, tex: List<IFramebuffer>) {
        for ((index, texture) in tex.withIndex().reversed()) {
            texture.bindTexture0(offset + index, filtering, clamping)
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
        val dst = Texture2D.bufferPool[a.remaining() * 2, false, false]
        val size = a.limit()
        for (i in 0 until size) {
            dst.put(a[i])
            dst.put(b[i])
        }
        dst.flip()
        Texture2D.bufferPool.returnBuffer(a)
        Texture2D.bufferPool.returnBuffer(b)
        return dst
    }

    abstract fun load(input: InputStream)

    open fun bindUVCorrection(shader: Shader) {
        val w = width
        val h = height
        shader.v2f("uvCorrection", w.toFloat() / ((w + 1) / 2 * 2), h.toFloat() / ((h + 1) / 2 * 2))
    }

    fun writeMonochromeDebugImage(w: Int, h: Int, buffer: ByteBuffer) {
        val file = findNextFile(desktop, "mono", "png", 1, '-')
        val image = ByteImage(w, h, ByteImage.Format.R)
        val data = image.data
        for (i in 0 until w * h) {
            data[i] = buffer[i]
        }
        image.write(file)
    }

    /**
     * Creates a new texture, which contains the image data of the frame
     * */
    fun toTexture(): Texture2D {
        return toTexture(Texture2D("GpuFrame", width, height, 1))
    }

    /**
     * Creates a new texture, which contains the image data of the frame
     * */
    fun toTexture(texture: Texture2D): Texture2D {
        GFX.checkIsGFXThread()
        texture.create(TargetType.UInt8xI[numChannels - 1])
        texture.channels = numChannels
        GFXState.useFrame(texture) {
            GFXState.renderPurely {
                val shader = get2DShader()
                shader.use()
                posSize(shader, 0, 0, texture.width, texture.height)
                tiling(shader, 1f, -1f, 0f, 0f)
                bind(0, Filtering.TRULY_LINEAR, Clamping.CLAMP)
                bindUVCorrection(shader)
                SimpleBuffer.flat01.draw(shader)
                GFX.check()
            }
        }
        GFX.check()
        return texture
    }

    fun warnAlreadyDestroyed() {
        LogManager.getLogger(this::class)
            .warn("Frame is already destroyed!")
    }

    companion object {
        @JvmField
        val creationLimiter = Semaphore(32)

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
        val swizzleStageMono = ShaderStage(
            "loadTex", listOf(
                Variable(GLSLType.V2F, "finalUV"),
                Variable(GLSLType.S2D, "tex"),
                Variable(GLSLType.V4F, "color", VariableMode.OUT)
            ), "color = vec4(getTexture(tex, finalUV).xxx,1.0);\n"
        )

        private val shaderMap2d = LazyMap<ShaderStage, Shader> { key ->
            Shader(
                "2d-${this::class.simpleName}",
                ShaderLib.uiVertexShaderList, ShaderLib.uiVertexShader,
                ShaderLib.uvList, key.variables.filter { !it.isOutput } + listOf(
                    Variable(GLSLType.V4F, "color", VariableMode.OUT),
                ), "" +
                        "vec4 getTexture(sampler2D tex, vec2 uv) {\n" +
                        "   return texture(tex,uv);\n" +
                        "}\n" +
                        "void main(){\n" +
                        "   vec2 finalUV = uv;\n" +
                        key.body +
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
                        "vec4 getTexture(sampler2D tex, vec2 uv) {\n" +
                        "   uv = (uv-0.5) * tiling.xy + 0.5 + tiling.zw;\n" +
                        "   return texture(tex, uv);\n" +
                        "}\n" +
                        "void main(){\n" +
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