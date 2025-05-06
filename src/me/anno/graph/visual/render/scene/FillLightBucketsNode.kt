package me.anno.graph.visual.render.scene

import me.anno.gpu.CullMode
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeLayout.Companion.bind
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.LightShaders.instancedLocalTransform
import me.anno.gpu.pipeline.LightShaders.positionCalculation
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsVertexShader
import me.anno.gpu.shader.ShaderLib.loadMat4x3
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.TextureLib
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.scene.RenderForwardPlusNode.Companion.bucketSize
import me.anno.graph.visual.render.scene.RenderForwardPlusNode.Companion.maxLightsPerTile
import me.anno.maths.Maths.ceilDiv
import me.anno.utils.Color
import org.lwjgl.opengl.GL46C.GL_SHADER_STORAGE_BARRIER_BIT
import org.lwjgl.opengl.GL46C.glMemoryBarrier
import kotlin.math.max
import kotlin.math.min

class FillLightBucketsNode : RenderViewNode(
    "FillLightBuckets", listOf(
        "Int", "Width",
        "Int", "Height",
    ), listOf(
        "Buffer", "LightBuckets",
        "Texture", "LightTextureDebug",
        "Int", "NumLightBucketsX",
        "Int", "NumLightBucketsY",
    )
) {

    companion object {

        private val intPseudoLayout = bind(Attribute("pseudo", AttributeType.UINT8, 1))

        val lightBucketSize = 16 + (16 * 6) * maxLightsPerTile
        private val lightBucketStructs = "" +
                "struct Light {\n" +
                "   vec4 lightData0;\n" +
                "   vec4 lightData1;\n" +
                "   vec4 lightData2;\n" +
                "   vec4 invTrans0;\n" +
                "   vec4 invTrans1;\n" +
                "   vec4 invTrans2;\n" +
                "};\n" +
                "struct LightBucket {\n" +
                "   int count;\n" +
                "   Light[${maxLightsPerTile}] lights;\n" +
                "};\n"

        private val lightBucketsDeclaration = lightBucketStructs +
                "layout(std430, binding=0) buffer lightBuckets1 { LightBucket lightBuckets[]; };\n"

        val lightBucketsReadonlyDeclaration = lightBucketStructs +
                "layout(std430, binding=0) readonly buffer lightBuckets1 { LightBucket lightBuckets[]; };\n"

        val getBucketId = "" +
                "int getBucketId(ivec2 pixelId) {\n" +
                "   ivec2 pixelId1 = clamp(pixelId, ivec2(0), numBuckets-1);\n" +
                "   return pixelId1.x + pixelId1.y * numBuckets.x;\n" +
                "}\n"

        /**
         * When the light is small, we might miss a few pixels, so extend it by 1-2 pixels on all sides.
         * */
        private val extendBy1Pixel = "" +
                "if (!isFullscreen) {\n" +
                "   vec3 center = isSpotLight ? vec3(0.0, 0.0, -0.5) : vec3(0.0);\n" +
                "   vec3 finalCenter = matMul(localTransform, vec4(center, 1.0));\n" +
                "   vec4 projCenter = matMul(transform, vec4(finalCenter, 1.0));\n" +
                "   vec2 ndc1 = gl_Position.xy / gl_Position.w;\n" +
                "   vec2 ndc0 = projCenter.xy / projCenter.w;\n" +
                "   vec2 factor = max(3.0 / (length(ndc0-ndc1) * vec2(numBuckets)), vec2(0.0));\n" +
                "   gl_Position.xy = mix(ndc1,ndc0,-factor) * gl_Position.w;\n" +
                "}\n"

        private val clearBucketsShader = Shader(
            "ClearBuckets", emptyList(), coordsVertexShader,
            emptyList(), listOf(
                Variable(GLSLType.V2I, "numBuckets"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    lightBucketsDeclaration +
                    getBucketId +
                    "void main() {\n" +
                    "   lightBuckets[getBucketId(ivec2(gl_FragCoord.xy))].count = 0;\n" +
                    "   result = vec4(0.0);\n" +
                    "}\n"
        ).apply { glslVersion = 430 }

        private val addLightToBucketShader = Shader(
            "AddLightToBucket", listOf(
                Variable(GLSLType.V3F, "positions", VariableMode.ATTR),
                Variable(GLSLType.M4x4, "transform"),
                Variable(GLSLType.M4x3, "localTransform"),
                Variable(GLSLType.V2I, "numBuckets"),
                Variable(GLSLType.V4F, "data0"),
                Variable(GLSLType.V4F, "data1"),
            ), "" +
                    "void main() {\n" +
                    positionCalculation +
                    extendBy1Pixel +
                    "}\n", emptyList(), listOf(
                Variable(GLSLType.V4F, "data0"),
                Variable(GLSLType.V4F, "data1"),
                Variable(GLSLType.V4F, "data2"),
                Variable(GLSLType.V1F, "debugGlow"),
                Variable(GLSLType.V2I, "numBuckets"),
                Variable(GLSLType.M4x3, "camSpaceToLightSpace"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    lightBucketsDeclaration +
                    getBucketId +
                    loadMat4x3 +
                    "void main() {\n" +
                    "   int bucketId = getBucketId(ivec2(gl_FragCoord.xy));\n" +
                    // atomic-ally increase bucket-fill-size
                    "   int lightIndex = atomicAdd(lightBuckets[bucketId].count,1);\n" +
                    "   if (lightIndex >= 0 && lightIndex < $maxLightsPerTile) {\n" +
                    // if there is still space, add the light
                    "       Light light;\n" +
                    "       light.lightData0 = data0;\n" +
                    "       light.lightData1 = data1;\n" +
                    "       light.lightData2 = data2;\n" +
                    "       storeMat4x3(camSpaceToLightSpace, light.invTrans0, light.invTrans1, light.invTrans2);\n" +
                    "       lightBuckets[bucketId].lights[lightIndex] = light;\n" +
                    "   }\n" +
                    "   result = vec4(debugGlow);\n" +
                    "}\n"
        ).apply { glslVersion = 430 }

        private val addLightToBucketShaderInstanced = Shader(
            "AddLightToBucketInstanced", listOf(
                Variable(GLSLType.V3F, "positions", VariableMode.ATTR),
                Variable(GLSLType.V4F, "instanceTrans0", VariableMode.ATTR),
                Variable(GLSLType.V4F, "instanceTrans1", VariableMode.ATTR),
                Variable(GLSLType.V4F, "instanceTrans2", VariableMode.ATTR),
                Variable(GLSLType.V4F, "invInsTrans0", VariableMode.ATTR),
                Variable(GLSLType.V4F, "invInsTrans1", VariableMode.ATTR),
                Variable(GLSLType.V4F, "invInsTrans2", VariableMode.ATTR),
                Variable(GLSLType.V4F, "lightData0", VariableMode.ATTR),
                Variable(GLSLType.V4F, "lightData1", VariableMode.ATTR),
                Variable(GLSLType.V2I, "numBuckets"),
                Variable(GLSLType.M4x4, "transform"),
            ), "" +
                    loadMat4x3 +
                    "void main() {\n" +
                    "   data0 = lightData0;\n" +
                    "   data1 = lightData1;\n" +
                    "   invInsTrans0v = invInsTrans0;\n" +
                    "   invInsTrans1v = invInsTrans1;\n" +
                    "   invInsTrans2v = invInsTrans2;\n" +
                    instancedLocalTransform +
                    positionCalculation +
                    extendBy1Pixel +
                    "}\n", listOf(
                Variable(GLSLType.V4F, "data0").flat(),
                Variable(GLSLType.V4F, "data1").flat(),
                Variable(GLSLType.V4F, "invInsTrans0v").flat(),
                Variable(GLSLType.V4F, "invInsTrans1v").flat(),
                Variable(GLSLType.V4F, "invInsTrans2v").flat(),
            ), listOf(
                Variable(GLSLType.V1F, "debugGlow"),
                Variable(GLSLType.V2I, "numBuckets"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    lightBucketsDeclaration +
                    loadMat4x3 +
                    getBucketId +
                    "void main() {\n" +
                    // calculate bucketId
                    "   int bucketId = getBucketId(ivec2(gl_FragCoord.xy));\n" +
                    // atomic-ally increase bucket-fill-size
                    "   int lightIndex = atomicAdd(lightBuckets[bucketId].count,1);\n" +
                    "   if (lightIndex >= 0 && lightIndex < $maxLightsPerTile) {\n" +
                    // if there is still space, add the light
                    "       Light light;\n" +
                    "       light.lightData0 = data0;\n" +
                    "       light.lightData1 = data1;\n" +
                    "       light.lightData2 = vec4(0.0);\n" +
                    "       light.invTrans0 = invInsTrans0v;\n" +
                    "       light.invTrans1 = invInsTrans1v;" +
                    "       light.invTrans2 = invInsTrans2v;\n" +
                    "       lightBuckets[bucketId].lights[lightIndex] = light;\n" +
                    "   }\n" +
                    "   result = vec4(debugGlow);\n" +
                    "}\n"
        ).apply { glslVersion = 430 }
    }

    private var lightBuckets: ComputeBuffer? = null

    override fun executeAction() {
        val width = getIntInput(1)
        val height = getIntInput(2)

        val numBucketsX = ceilDiv(width, bucketSize)
        val numBucketsY = ceilDiv(height, bucketSize)
        if (numBucketsX <= 0 || numBucketsY <= 0) return

        val numBuckets = numBucketsX * numBucketsY
        val numValues = numBuckets * lightBucketSize
        var lightBuckets = lightBuckets
        if (lightBuckets == null || lightBuckets.elementCount != numValues) {
            lightBuckets?.destroy()
            lightBuckets = ComputeBuffer("LightBuckets", intPseudoLayout, numValues)
            this.lightBuckets = lightBuckets
        }

        var framebuffer = framebuffer
        if (framebuffer !is Framebuffer || framebuffer.width != numBucketsX || framebuffer.height != numBucketsY) {
            framebuffer?.destroy()
            if (framebuffer !is Framebuffer) {
                framebuffer = Framebuffer(
                    "UnusedForLightBuckets", numBucketsX, numBucketsY,
                    1, TargetType.UInt8x1, DepthBufferType.NONE
                )
                this.framebuffer = framebuffer
            } else {
                framebuffer.width = numBucketsX
                framebuffer.height = numBucketsY
            }
        }

        timeRendering(name, timer) {
            useFrame(framebuffer) {
                // clear buckets and framebuffer
                GFXState.blendMode.use(null) {
                    val shader0 = clearBucketsShader
                    shader0.use()
                    shader0.v2i("numBuckets", numBucketsX, numBucketsY)
                    shader0.bindBuffer(0, lightBuckets)
                    flat01.draw(shader0)
                }

                glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT)

                // render lights into buckets
                GFXState.cullMode.use(CullMode.BACK) {
                    GFXState.blendMode.use(BlendMode.PURE_ADD) {

                        var numLightsForGlow = pipeline.lightStage.size
                        numLightsForGlow = min(maxLightsPerTile.toLong(), numLightsForGlow)
                        numLightsForGlow = max(numLightsForGlow, 4)

                        pipeline.lightStage.draw(
                            pipeline, { lightType, isInstanced ->
                                val shader =
                                    if (isInstanced) addLightToBucketShaderInstanced
                                    else addLightToBucketShader
                                shader.use()
                                shader.v2i("numBuckets", numBucketsX, numBucketsY)
                                shader.v1f("debugGlow", 1f / numLightsForGlow)
                                shader.bindBuffer(0, lightBuckets)
                                shader
                            },
                            // todo can we get a down-scaled depth-texture? :) maybe the one from BoxCulling
                            TextureLib.depthTexture, Color.black4
                        )
                    }
                }

                glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT)
            }
        }

        setOutput(1, lightBuckets)
        setOutput(2, Texture.texture(framebuffer, 0))
        setOutput(3, numBucketsX)
        setOutput(4, numBucketsY)
    }

    override fun destroy() {
        super.destroy()
        lightBuckets?.destroy()
        lightBuckets = null
    }
}