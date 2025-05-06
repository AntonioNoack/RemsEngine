package me.anno.graph.visual.render.scene

import me.anno.ecs.components.light.LightType
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToLinear
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToSRGB
import me.anno.engine.ui.render.RendererLib.combineLightCode
import me.anno.engine.ui.render.RendererLib.defineLightVariables
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.engine.ui.render.RendererLib.sampleSkyboxForAmbient
import me.anno.engine.ui.render.RendererLib.skyMapCode
import me.anno.engine.ui.render.Renderers.MAX_CUBEMAP_LIGHTS
import me.anno.engine.ui.render.Renderers.MAX_PLANAR_LIGHTS
import me.anno.engine.ui.render.Renderers.finalResultStage
import me.anno.engine.ui.render.Renderers.tonemapGLSL
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.timeRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.LightShaders.addDiffuseLight
import me.anno.gpu.pipeline.LightShaders.addSpecularLight
import me.anno.gpu.pipeline.LightShaders.startLightSum
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.shader.BaseShader.Companion.IS_DEFERRED
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib.randomGLSL
import me.anno.gpu.shader.ShaderLib.brightness
import me.anno.gpu.shader.ShaderLib.loadMat4x3
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.graph.visual.render.Texture
import me.anno.maths.Maths.clamp
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Vector2i

/**
 * Implements forward+ rendering.
 * Only supports a single stage at the moment.
 * Requires FillLightBucketsNode to be executed before it.
 *
 * Doesn't support shadows yet.
 * todo we need to find & bind shadowMaps somehow...
 * todo we need to map shadow indices when collecting the lights
 *   can we use bindless textures? might help a lot, too
 *   textures with lots of shadow maps could use two memory slots, if we need that
 * */
class RenderForwardPlusSceneNode() : RenderViewNode(
    "Forward+ FillLights", listOf(
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Enum<me.anno.gpu.pipeline.PipelineStage>", "Stage",
        "Boolean", "Apply Tone Mapping",
        "Int", "Skybox Resolution", // or 0 to not bake it
        "Buffer", "LightBuckets",
        "Int", "LightBucketsX",
        "Int", "LightBucketsY",
    ), listOf(
        "Texture", "Illuminated",
        "Texture", "Depth",
    )
) {

    companion object {

        val bucketSize = 32
        val maxLightsPerTile = 32

        val numBuckets = Vector2i(1)
        var bucket: ComputeBuffer? = null

        @JvmField
        val forwardPlusRenderer = object : Renderer(
            "Forward+", DeferredSettings(
                listOf(DeferredLayerType.COLOR, DeferredLayerType.ALPHA) +
                        (if (GFX.supportsDepthTextures) emptyList() else listOf(DeferredLayerType.DEPTH))
            )
        ) {

            override fun bind(shader: Shader) {
                super.bind(shader)
                shader.v2i("numBuckets", numBuckets)
                shader.bindBuffer(0, bucket!!)
            }

            override fun getPixelPostProcessing(flags: Int): List<ShaderStage> {
                return listOf(
                    ShaderStage(
                        "forward+", listOf(

                            // rendering
                            Variable(GLSLType.V1B, "applyToneMapping"),
                            // light data
                            Variable(GLSLType.V1I, "numberOfLights"),
                            Variable(GLSLType.V1B, "receiveShadows"),
                            Variable(GLSLType.V1B, "canHaveShadows"),
                            Variable(GLSLType.V2I, "numBuckets"),

                            // light maps for shadows
                            // - spotlights, directional lights
                            Variable(GLSLType.S2DAShadow, "shadowMapPlanar", MAX_PLANAR_LIGHTS),
                            // - point lights
                            Variable(GLSLType.SCubeShadow, "shadowMapCubic", MAX_CUBEMAP_LIGHTS),

                            // reflection plane for rivers or perfect mirrors
                            Variable(GLSLType.V1B, "hasReflectionPlane"),
                            Variable(GLSLType.S2D, "reflectionPlane"),
                            // reflection cubemap or irradiance map
                            Variable(GLSLType.SCube, "reflectionMap"),
                            // material properties
                            Variable(GLSLType.V3F, "finalEmissive", VariableMode.INOUT),
                            Variable(GLSLType.V1F, "finalMetallic"),
                            Variable(GLSLType.V1F, "finalReflectivity"),
                            Variable(GLSLType.V1F, "finalSheen"),
                            // if the translucency > 0, the normal map probably should be turned into occlusion ->
                            // no, or at max slightly, because the surrounding area will illuminate it
                            Variable(GLSLType.V1F, "finalTranslucency"),
                            Variable(GLSLType.V1F, "finalAlpha"),
                            Variable(GLSLType.V3F, "finalPosition"),
                            Variable(GLSLType.V3F, "finalNormal"),
                            Variable(GLSLType.V1F, "finalOcclusion"),
                            Variable(GLSLType.V3F, "finalColor", VariableMode.INOUT),
                            Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
                        ), "" +
                                // define all light positions, radii, types and colors
                                // use the lights to illuminate the model
                                // light data
                                // a try of depth dithering, which can be used for plants, but is really expensive...
                                // "   gl_FragDepth = 1.0/(1.0+zDistance) * (1.0 + 0.001 * random(finalPosition.xy));\n" +
                                // shared pbr data
                                "#ifndef SKIP_LIGHTS\n" +
                                colorToLinear +
                                "   vec3 V = normalize(-finalPosition);\n" +
                                // light calculations
                                "   float NdotV = abs(dot(finalNormal,V));\n" +
                                "   vec3 finalColor0 = finalColor;\n" +
                                startLightSum +
                                "   float NdotL = 0.0;\n" + // normal dot light
                                "   int bucketId = getBucketId(ivec2(gl_FragCoord.xy)/$bucketSize);\n" +
                                "   int numberOfLights = lightBuckets[bucketId].count;\n" +
                                "   for(int i=0;i<numberOfLights;i++){\n" +
                                "       Light light = lightBuckets[bucketId].lights[i];\n" +
                                "       mat4x3 camSpaceToLightSpace = loadMat4x3(light.invTrans0,light.invTrans1,light.invTrans2);\n" +
                                "       vec3 lightDir = vec3(0.0,0.0,-1.0);\n" +
                                // local space, for falloff and such
                                "       vec3 lightPos = matMul(camSpaceToLightSpace, vec4(finalPosition,1.0));\n" +
                                "       vec3 lightNor = normalize(matMul(camSpaceToLightSpace, vec4(finalNormal,0.0)));\n" +
                                "       vec3 viewDir = normalize(matMul(camSpaceToLightSpace, vec4(finalPosition, 0.0)));\n" +
                                "       vec3 effectiveDiffuse = vec3(0.0), effectiveSpecular = vec3(0.0);\n" +
                                "       vec4 data0 = light.lightData0;\n" +
                                "       vec4 data1 = light.lightData1;\n" +
                                "       vec4 data2 = vec4(0.0); // light.lightData2;\n" + // shadows not yet supported
                                defineLightVariables +
                                // local coordinates of the point in the light "cone"
                                // removed switch(), because WebGL had issues with continue inside it...
                                LightType.entries.joinToString("") { type ->
                                    val start = if (type.ordinal == 0) "if" else " else if"
                                    val cutoffKeyword = "continue"
                                    val withShadows = true
                                    "$start(lightType == ${type.id}){${
                                        LightType.getShaderCode(type, cutoffKeyword, withShadows)
                                    }}"
                                } + "\n" +
                                addSpecularLight +
                                addDiffuseLight +
                                "   }\n" +
                                combineLightCode +
                                (if (flags.hasFlag(IS_DEFERRED)) "" else skyMapCode) +
                                "#endif\n" +
                                colorToLinear +
                                "   if(applyToneMapping) finalColor = tonemapLinear(finalColor);\n" +
                                colorToSRGB +
                                "   finalResult = vec4(finalColor, finalAlpha);\n"
                    ).add(randomGLSL).add(tonemapGLSL).add(getReflectivity).add(sampleSkyboxForAmbient)
                        .add(brightness).add(FillLightBucketsNode.Companion.getBucketId).add(FillLightBucketsNode.Companion.lightBucketsReadonlyDeclaration).add(loadMat4x3),
                    finalResultStage
                )
            }
        }
    }

    init {
        setInput(1, 256)
        setInput(2, 256)
        setInput(3, 1)
        setInput(4, PipelineStage.OPAQUE)
        setInput(5, false)
        setInput(6, 256)
    }

    val width get() = getIntInput(1)
    val height get() = getIntInput(2)
    val samples get() = clamp(getIntInput(3), 1, GFX.maxSamples)
    val stage get() = getInput(4) as PipelineStage
    val applyToneMapping get() = getBoolInput(5)
    val skyResolution get() = getIntInput(6)
    val bucketInput get() = getInput(7) as ComputeBuffer
    val width2 get() = getIntInput(8)
    val height2 get() = getIntInput(9)

    override fun executeAction() {
        val width = width
        val height = height
        val samples = samples
        if (width <= 0 || height <= 0) return

        var framebuffer = framebuffer
        if (framebuffer !is Framebuffer ||
            framebuffer.width != width ||
            framebuffer.height != height ||
            framebuffer.samples != samples
        ) {
            framebuffer?.destroy()
            if (framebuffer !is Framebuffer || framebuffer.samples != samples) {
                framebuffer = Framebuffer(
                    "Forward+ Render", width, height, samples,
                    TargetType.Float16x4, DepthBufferType.TEXTURE
                )
                this.framebuffer = framebuffer
            } else {
                framebuffer.width = width
                framebuffer.height = height
            }
        }

        numBuckets.set(width2, height2)
        bucket = bucketInput
        // verify that the bucket has enough data
        assertTrue(
            bucketInput.pointer >= 0 &&
                    bucketInput.elementCount >= width2 * height2 * FillLightBucketsNode.Companion.lightBucketSize
        )

        timeRendering(name, timer) {
            pipeline.bakeSkybox(skyResolution)
            useFrame(framebuffer, forwardPlusRenderer) {
                GFXState.depthMode.use(renderView.depthMode) {
                    framebuffer.clearDepth()
                }
                val prevApplyToneMapping = pipeline.applyToneMapping
                pipeline.applyToneMapping = applyToneMapping
                pipeline.stages.getOrNull(stage.id)
                    ?.bindDraw(pipeline)
                pipeline.drawSky()
                pipeline.applyToneMapping = prevApplyToneMapping // needed???
            }
        }

        setOutput(1, Texture.texture(framebuffer, 0))
        setOutput(2, Texture.depth(framebuffer))
    }
}
