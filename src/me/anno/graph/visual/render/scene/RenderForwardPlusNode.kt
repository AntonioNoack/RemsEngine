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
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.LightShaders.addDiffuseLight
import me.anno.gpu.pipeline.LightShaders.addSpecularLight
import me.anno.gpu.pipeline.LightShaders.startLightSum
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.PipelineStageImpl
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
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.graph.visual.render.scene.RenderForwardNode.Companion.copyInputs
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
class RenderForwardPlusNode() : RenderViewNode(
    "RenderSceneForward+", listOf(
        // usual rendering inputs
        "Int", "Width",
        "Int", "Height",
        "Int", "Samples",
        "Enum<me.anno.gpu.pipeline.PipelineStage>", "Stage",
        "Boolean", "Apply Tone Mapping",
        "Int", "Skybox Resolution", // or 0 to not bake it
        "Enum<me.anno.graph.visual.render.scene.DrawSkyMode>", "Draw Sky",
        // previous data
        "Texture", "Illuminated",
        "Texture", "Depth",
        // light buckets input
        "Buffer", "LightBuckets",
        "Int", "NumLightBucketsX",
        "Int", "NumLightBucketsY",
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
                        .add(brightness).add(FillLightBucketsNode.Companion.getBucketId)
                        .add(FillLightBucketsNode.Companion.lightBucketsReadonlyDeclaration).add(loadMat4x3),
                    finalResultStage
                )
            }
        }
    }

    init {
        setInput(1, 256) // width
        setInput(2, 256) // height
        setInput(3, 1) // samples
        setInput(4, PipelineStage.OPAQUE)
        setInput(5, false) // apply tonemapping
        setInput(6, 0) // skybox resolution
        setInput(7, DrawSkyMode.DONT_DRAW_SKY)
    }

    // usual data
    val width get() = getIntInput(1)
    val height get() = getIntInput(2)
    val samples get() = clamp(getIntInput(3), 1, GFX.maxSamples)
    val stage get() = getInput(4) as PipelineStage
    val applyToneMapping get() = getBoolInput(5)
    val skyResolution get() = getIntInput(6)
    val drawSky get() = getInput(7) as DrawSkyMode

    // previous data
    val prepassColor get() = getInput(8) as? Texture
    val prepassDepth get() = getInput(9) as? Texture

    // light bucket data
    val bucketInput get() = getInput(10) as ComputeBuffer
    val numBucketsX get() = getIntInput(11)
    val numBucketsY get() = getIntInput(12)

    val stageImpl get() = pipeline.stages.getOrNull(stage.id)

    override fun executeAction() {
        if (width <= 0 || height <= 0) return
        timeRendering(name, timer) {
            executeRendering(stageImpl)
        }
    }

    private fun prepareFramebuffer(): IFramebuffer {
        return FBStack["scene-forwardPlus", width, height, TargetType.Float16x4, samples, DepthBufferType.TEXTURE]
    }

    fun executeRendering(stageImpl: PipelineStageImpl?) {

        numBuckets.set(numBucketsX, numBucketsY)
        bucket = bucketInput
        // verify that the bucket has enough data
        assertTrue(
            bucketInput.pointer >= 0 &&
                    bucketInput.elementCount >= numBucketsX * numBucketsY * FillLightBucketsNode.Companion.lightBucketSize
        )

        pipeline.bakeSkybox(skyResolution)
        pipeline.applyToneMapping = applyToneMapping

        val drawSky = drawSky
        val framebuffer = prepareFramebuffer()
        GFXState.depthMode.use(renderView.depthMode) {
            copyInputs(framebuffer, prepassColor.texOrNull, prepassDepth)
            useFrame(framebuffer, forwardPlusRenderer) {
                if (drawSky == DrawSkyMode.BEFORE_GEOMETRY) pipeline.drawSky()
                if (stageImpl != null && !stageImpl.isEmpty()) stageImpl.bindDraw(pipeline)
                if (drawSky == DrawSkyMode.AFTER_GEOMETRY) pipeline.drawSky()
                GFX.check()
            }
        }

        if (framebuffer.depthBufferType != DepthBufferType.NONE) {
            pipeline.prevDepthBuffer = framebuffer
        }

        setOutput(1, Texture.texture(framebuffer, 0))
        setOutput(2, Texture.depth(framebuffer))
    }
}
