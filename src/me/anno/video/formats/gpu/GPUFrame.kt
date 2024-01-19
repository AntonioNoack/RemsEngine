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
import me.anno.utils.Sleep.waitForGFXThread
import me.anno.utils.files.Files.findNextFile
import me.anno.utils.structures.maps.LazyMap
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.Semaphore

abstract class GPUFrame(var width: Int, var height: Int, var numChannels: Int, val code: Int) : ICacheData {

    init {
        if (width < 1 || height < 1) throw IllegalArgumentException("Cannot create empty frames")
    }

    val isCreated: Boolean get() = getTextures().all { it.wasCreated && !it.isDestroyed }
    val isDestroyed: Boolean get() = getTextures().any { it.isDestroyed }

    val blankDetector = BlankFrameDetector()

    fun isBlankFrame(f0: GPUFrame, f4: GPUFrame, outlierThreshold: Float = 1f): Boolean {
        return blankDetector.isBlankFrame(f0.blankDetector, f4.blankDetector, outlierThreshold)
    }

    fun get3DShaderPlanar(): BaseShader {
        val key = getShaderStage()
        return shaderMap3d.getOrPut(key) {
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

    fun get2DShader(): Shader {
        val key = getShaderStage()
        return shaderMap2d.getOrPut(key) {
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
    }

    abstract fun getShaderStage(): ShaderStage

    abstract fun getTextures(): List<Texture2D>

    abstract fun bind(offset: Int, nearestFiltering: Filtering, clamping: Clamping)

    abstract fun getByteSize(): Long

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
    fun waitToLoad() {
        waitForGFXThread(true) { isCreated || isDestroyed }
    }

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
        GFXState.useFrame(texture, 0) {
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

    companion object {
        @JvmField
        val creationLimiter = Semaphore(32)
        val frameAlreadyDestroyed = "Frame is already destroyed!"
        val swizzleStages = LazyMap<String, ShaderStage> { swizzle ->
            ShaderStage(
                "YUV", listOf(
                    Variable(GLSLType.V2F, "finalUV"),
                    Variable(GLSLType.S2D, "tex"),
                    Variable(GLSLType.V4F, "color", VariableMode.OUT)
                ), "color = getTexture(tex, finalUV)$swizzle;\n"
            )
        }
        val shaderMap2d = HashMap<ShaderStage, Shader>()
        val shaderMap3d = HashMap<ShaderStage, BaseShader>()
    }
}