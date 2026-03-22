package me.anno.graph.visual.render.effects

import me.anno.Time
import me.anno.gpu.GFX.INVALID_POINTER
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib.brightness
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.ITexture2D
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.texMSOrNull
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.graph.visual.render.effects.ToneMappingNode.Companion.EXPOSURE_NAME
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.posMod
import me.anno.utils.search.Histogram.getHistogramIndex
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.iff
import org.joml.Vector3i
import org.lwjgl.opengl.GL46C.GL_BUFFER_UPDATE_BARRIER_BIT
import org.lwjgl.opengl.GL46C.GL_ELEMENT_ARRAY_BARRIER_BIT
import org.lwjgl.opengl.GL46C.GL_SHADER_STORAGE_BARRIER_BIT
import org.lwjgl.opengl.GL46C.GL_TEXTURE_FETCH_BARRIER_BIT
import org.lwjgl.opengl.GL46C.glMemoryBarrier
import kotlin.math.log2
import kotlin.math.pow

// todo we should also apply desaturation at low exposure levels...
//  implement that somehow...

// todo bug:
//  we have quite a lot of noise when exposure is very low...
//  probably randomness not expected so small values ...

class AutoExposureNode : TimedRenderingNode(
    "AutoExposure",
    listOf(
        "Float", "Percentile", // 0 .. 100
        "Float", "Scale",
        "Texture", "Illuminated",
        // applyToneMapping = scale / colors[percentile]
    ), listOf("Float", EXPOSURE_NAME)
) {

    init {
        setInput(1, 60f)
        setInput(2, 1f)
    }

    var exposure = 1f
    var adaptionSpeed = 3f

    override fun executeAction() {

        val scale = getFloatInput(2)
        if (scale <= 0f) return setOutput(1, 0f)

        val percentile = clamp(getFloatInput(1) * 0.01f)

        val colorT = getInput(3) as? Texture
        val colorMT = colorT?.texMSOrNull ?: colorT?.texOrNull

        if (colorMT != null) {
            timeRendering(name, timer) {
                countPixels(colorMT)
                evaluateBrightness(percentile, scale)
            }
        }

        setOutput(1, exposure)
    }

    private fun countPixels(texture: ITexture2D) {

        val writeBuffer = buffers[posMod(Time.frameIndex, buffers.size)]
        if (writeBuffer.pointer == INVALID_POINTER) writeBuffer.uploadEmpty(numBins * 4L)

        writeBuffer.ensureBuffer()
        writeBuffer.zeroElements(0, numBins)

        glMemoryBarrier(BARRIER_BITS or GL_TEXTURE_FETCH_BARRIER_BIT)

        val shader = shader[(texture.samples > 1).toInt()]
        shader.use()
        shader.v1f("minLogLum", log2(minBrightness))
        shader.v1f("maxLogLum", log2(maxBrightness))
        texture.bindTrulyNearest(shader, "colorTex")

        shader.bindBuffer(0, writeBuffer)
        shader.runBySize(texture.width, texture.height)

        glMemoryBarrier(BARRIER_BITS)
    }

    private fun evaluateBrightness(percentile: Float, scale: Float) {
        val readBuffer = buffers[posMod(Time.frameIndex + 1, buffers.size)]
        if (readBuffer.pointer == INVALID_POINTER) return

        val values = readBuffer.readAsIntArray(0, tmpBins)
        if (values.any { it < 0 } || values.all { it == 0 }) return

        val brightness = findPercentile(values, percentile)
        val targetExposure = scale / brightness
        val adaption = dtTo01(Time.deltaTime.toFloat() * adaptionSpeed)
        exposure = mix(exposure, targetExposure, adaption)
    }

    private fun findPercentile(values: IntArray, percentile: Float): Float {
        val numStops = log2(maxBrightness) - log2(minBrightness)
        val relativeStops = getHistogramIndex(values, percentile) / numBins * numStops
        return minBrightness * 2f.pow(relativeStops)
    }

    private val buffers = Array(3) {
        ComputeBuffer("autoExposure", layout, numBins)
    }

    override fun destroy() {
        super.destroy()
        for (buffer in buffers) buffer.destroy()
    }

    companion object {

        // no reason to change this
        private val numBins = 256
        private val tmpBins = IntArray(numBins)

        // could be application-dependent
        var minBrightness = 1e-6f
        var maxBrightness = 1e6f

        private const val BARRIER_BITS =
            GL_SHADER_STORAGE_BARRIER_BIT or
                    GL_BUFFER_UPDATE_BARRIER_BIT or
                    GL_ELEMENT_ARRAY_BARRIER_BIT

        private val layout = bind(Attribute("count", AttributeType.UINT32, 1))

        // todo implement non-compute shader for WebGL
        private val shader = Array(2) { variant ->
            val msaa = variant.hasFlag(1)
            val localSize = Vector3i(32, 32, 1)
            ComputeShader(
                "autoExposure", localSize,
                listOf(
                    Variable(GLSLType.V1F, "minLogLum"),
                    Variable(GLSLType.V1F, "maxLogLum"),
                    Variable(if (msaa) GLSLType.S2DMS else GLSLType.S2D, "colorTex"),
                    Variable(GLSLType.BUFFER, "Histogram", numBins)
                        .defineBufferFormat("uint bins[$numBins];")
                        .binding(0)
                ), "" +
                        brightness +
                        "shared uint localBins[$numBins];\n" +
                        "void main() {\n" +
                        // Init shared histogram
                        "    for (uint i = gl_LocalInvocationIndex; i < $numBins; i += ${localSize.product()}) {\n" +
                        "        localBins[i] = 0;\n" +
                        "    }\n" +
                        "    barrier();\n" +

                        // Collect some pixels
                        "    ivec2 size = textureSize(colorTex${",0".iff(!msaa)});\n" +
                        "    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);\n" +
                        "    if (coord.x < size.x && coord.y < size.y) {\n" +
                        "        vec3 color = texelFetch(colorTex, coord${",0".iff(!msaa)}).rgb;\n" +
                        "        float logLum = 0.5 * log2(max(brightnessSq(color), 1e-36));\n" + // 0.5 for sqrt
                        "        float normalized = (logLum - minLogLum) * $numBins.0 / (maxLogLum - minLogLum);\n" +
                        "        int binIndex = int(clamp(normalized, 0.0, ${numBins - 2f}));\n" +
                        "        float delta = clamp(normalized-float(binIndex), 0.0, 1.0);\n" +
                        // add fractional index for smooth transitions
                        "        int value1 = int(100.0 * delta);\n" +
                        "        atomicAdd(localBins[binIndex  ], uint(100 - value1));\n" +
                        "        atomicAdd(localBins[binIndex+1], uint(value1));\n" +
                        "    }\n" +

                        "    barrier();\n" +

                        // Merge into global histogram
                        "    for (uint i = gl_LocalInvocationIndex; i < $numBins; i += ${localSize.product()}) {\n" +
                        "        atomicAdd(bins[i], localBins[i]);\n" +
                        "    }\n" +
                        "}"
            ).apply { setTextureIndices("colorTex") }
        }
    }
}