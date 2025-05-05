package me.anno.tests.gfx.forwardplus

import me.anno.gpu.CullMode
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.LightShaders.visualizeLightCountShader
import me.anno.gpu.pipeline.LightShaders.visualizeLightCountShaderInstanced
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsVertexShader
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.TextureLib
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.scene.RenderViewNode
import me.anno.maths.Maths.ceilDiv
import me.anno.utils.Color
import me.anno.utils.structures.lists.LazyList
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt

class FillLightBucketsNode : RenderViewNode(
    "Forward+ FillLights", listOf(
        "Int", "Width",
        "Int", "Height",
    ), listOf(
        "Buffer", "LightBuckets",
        "Texture", "LightTextureDebug"
    )
) {

    companion object {
        private val clearBucketsShader = Shader(
            "ClearBuckets", emptyList(), coordsVertexShader,
            emptyList(), listOf(
                Variable(GLSLType.V3I, "numBuckets"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    "layout(std430, binding=0) buffer lightBuckets1 { int lightBuckets[]; };\n" +
                    "void main() {\n" +
                    // calculate bucketId
                    "   ivec2 pixelId = ivec2(gl_FragCoord.xy);\n" +
                    "   if (pixelId.x >= 0 && pixelId.x < numBuckets.x &&" +
                    "       pixelId.y >= 0 && pixelId.y < numBuckets.y) {\n" +
                    "       int maxLightsPerTile = numBuckets.z;\n" +
                    "       int bucketId = pixelId.x + numBuckets.x * pixelId.y;\n" +
                    "       int bucketOffset = bucketId * maxLightsPerTile;\n" +
                    // clear bucket-fill-size
                    "       lightBuckets[bucketOffset] = 0;\n" +
                    "   }\n" +
                    "   result = vec4(0.0);\n" +
                    "}\n"
        ).apply { glslVersion = 430 }

        private val addLightToBucketShader = LazyList(2) { key ->
            val isInstanced = key.hasFlag(1)
            val base = if (isInstanced) visualizeLightCountShaderInstanced else visualizeLightCountShader
            Shader(
                "AddLightToBucket", base.vertexVariables, base.vertexShader,
                emptyList(), listOf(
                    Variable(GLSLType.V1I, "lightID"), // todo define lightID somehow...
                    Variable(GLSLType.V3I, "numBuckets"),
                    Variable(GLSLType.V4F, "result", VariableMode.OUT)
                ), "" +
                        "layout(std430, binding=0) buffer lightBuckets1 { int lightBuckets[]; };\n" +
                        "void main() {\n" +
                        // calculate bucketId
                        "   ivec2 pixelId = ivec2(gl_FragCoord.xy);\n" +
                        "   if (pixelId.x >= 0 && pixelId.x < numBuckets.x &&" +
                        "       pixelId.y >= 0 && pixelId.y < numBuckets.y) {\n" +
                        "       int maxLightsPerTile = numBuckets.z;\n" +
                        "       int bucketId = pixelId.x + numBuckets.x * pixelId.y;\n" +
                        "       int bucketOffset = bucketId * maxLightsPerTile;\n" +
                        // atomic-ally increase bucket-fill-size
                        "       int lightIndex = atomicAdd(lightBuckets[bucketOffset],1) + 1;\n" +
                        "       if (lightIndex > 0 && lightIndex < maxLightsPerTile) {\n" +
                        // add light-ID into there
                        "           lightBuckets[bucketOffset + lightIndex] = lightID;\n" +
                        "       }\n" +
                        "   }\n" +
                        "   result = vec4(0.25);" + // debug-glow
                        "}\n"
            ).apply { glslVersion = 430 }
        }
    }

    private var lightBuckets: ComputeBuffer? = null

    override fun executeAction() {
        val width = getIntInput(1)
        val height = getIntInput(2)

        val width2 = ceilDiv(width, bucketSize)
        val height2 = ceilDiv(height, bucketSize)
        if (width2 <= 0 || height2 <= 0) return

        val numBuckets = width2 * height2
        val numValues = numBuckets * maxLightsPerTile
        var lightBuckets = lightBuckets
        if (lightBuckets == null || lightBuckets.elementCount != numValues) {
            lightBuckets?.destroy()
            lightBuckets = ComputeBuffer("LightBuckets", lightBucketLayout, numValues)
            this.lightBuckets = lightBuckets
        }

        var framebuffer = framebuffer
        if (framebuffer !is Framebuffer || framebuffer.width != width2 || framebuffer.height != height2) {
            framebuffer?.destroy()
            if (framebuffer !is Framebuffer) {
                framebuffer = Framebuffer(
                    "UnusedForLightBuckets", width2, height2,
                    1, TargetType.UInt8x1, DepthBufferType.NONE
                )
            } else {
                framebuffer.width = width2
                framebuffer.height = height2
            }
        }

        timeRendering(name, timer) {
            useFrame(framebuffer) {
                // clear buckets and framebuffer
                GFXState.blendMode.use(null) {
                    val shader0 = clearBucketsShader
                    shader0.use()
                    shader0.v3i("numBuckets", width2, height2, maxLightsPerTile)
                    shader0.bindBuffer(0, lightBuckets)
                    flat01.draw(shader0)
                }

                // render lights into buckets
                GFXState.cullMode.use(CullMode.BACK) {
                    GFXState.blendMode.use(BlendMode.PURE_ADD) {
                        pipeline.lightStage.draw(
                            pipeline, { lightType, isInstanced ->
                                val shader = addLightToBucketShader[isInstanced.toInt()]
                                shader.use()
                                shader.v3i("numBuckets", width2, height2, maxLightsPerTile)
                                shader.bindBuffer(0, lightBuckets)
                                shader
                            },
                            // todo can we get a down-scaled depth-texture? :) maybe the one from BoxCulling
                            TextureLib.depthTexture, Color.black4
                        )
                    }
                }
            }
        }

        setOutput(1, lightBuckets)
        setOutput(2, Texture.texture(framebuffer, 0))
    }

    override fun destroy() {
        super.destroy()
        lightBuckets?.destroy()
        lightBuckets = null
    }
}