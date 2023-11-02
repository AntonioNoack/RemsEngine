package me.anno.engine.ui.render

import me.anno.engine.pbr.PBRLibraryGLTF.specularBRDFv2NoDivInlined2
import me.anno.engine.pbr.PBRLibraryGLTF.specularBRDFv2NoDivInlined2End
import me.anno.engine.pbr.PBRLibraryGLTF.specularBRDFv2NoDivInlined2Start
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToLinear
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToSRGB
import me.anno.engine.ui.render.RendererLib.combineLightCode
import me.anno.engine.ui.render.RendererLib.lightCode
import me.anno.engine.ui.render.RendererLib.sampleSkyboxForAmbient
import me.anno.engine.ui.render.RendererLib.skyMapCode
import me.anno.gpu.GFX
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.shader.BaseShader.Companion.IS_DEFERRED
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib.randomGLSL
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.octNormalPacking
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.SimpleRenderer
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.maths.Maths.hasFlag
import me.anno.maths.Maths.length
import me.anno.utils.pooling.ByteBufferPool
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.maps.LazyMap
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.max

object Renderers {

    // reinhard tonemapping works often, but not always: {0,1}³ does not work, add spilling somehow
    // also maybe it should be customizable...

    val finalResultStage = ShaderStage(
        "finalResult",
        listOf(Variable(GLSLType.V4F, "finalResult", VariableMode.INOUT)), ""
    )

    @JvmField
    var tonemapGLSL = "" +
            "vec3 tonemapLinear(vec3 color){\n" +
            "   float maxTerm = max(max(color.r, color.g), color.b);\n" +
            "   color = mix(vec3(1.0), color / (1.0 + maxTerm), 1.0/(1.0 + maxTerm * maxTerm * 0.0003));\n" +
            "   return color;\n" +
            "}\n" +
            "vec3 tonemap(vec3 color){\n" +
            "   color = pow(color,vec3(2.2));\n" +
            "   color = tonemapLinear(color);\n" +
            "   color = pow(color,vec3(1.0/2.2));\n" +
            "   return color;\n" +
            "}\n" +
            "vec4 tonemap(vec4 color){ return vec4(tonemap(color.rgb), color.a); }\n"

    @JvmField
    var tonemapKt = { color: Vector3f ->
        color.div(1f + max(max(color.x, color.y), max(color.z, 0f)))
    }

    @JvmField
    var tonemapInvKt = { color: Vector3f ->
        color.div(1f - max(max(color.x, color.y), max(color.z, 0f)))
    }

    @JvmStatic
    fun tonemapKt(color: Vector4f): Vector4f {
        val tmp = JomlPools.vec3f.create()
        tmp.set(color.x, color.y, color.z)
        tonemapKt(tmp)
        color.set(tmp.x, tmp.y, tmp.z)
        JomlPools.vec3f.sub(1)
        return color
    }

    @JvmStatic
    fun tonemapInvKt(color: Vector4f): Vector4f {
        val tmp = JomlPools.vec3f.create()
        tmp.set(color.x, color.y, color.z)
        tonemapInvKt(tmp)
        color.set(tmp.x, tmp.y, tmp.z)
        JomlPools.vec3f.sub(1)
        return color
    }

    @JvmField
    val overdrawRenderer = SimpleRenderer(
        "overdraw", ShaderStage(
            "overdraw", listOf(Variable(GLSLType.V4F, "finalOverdraw", VariableMode.OUT)),
            "finalOverdraw = vec4(0.125);\n"
        )
    )

    // same functionality :D
    @JvmField
    val cheapRenderer = overdrawRenderer

    @JvmField
    val pbrRenderer = object : Renderer("pbr") {
        override fun getPostProcessing(flags: Int): List<ShaderStage> {
            return listOf(
                ShaderStage(
                    "pbr", listOf(
                        // rendering
                        Variable(GLSLType.V1B, "applyToneMapping"),
                        // light data
                        Variable(GLSLType.V1I, "numberOfLights"),
                        Variable(GLSLType.V1B, "receiveShadows"),
                        Variable(GLSLType.M4x3, "lightMatrices", RenderView.MAX_FORWARD_LIGHTS),
                        Variable(GLSLType.M4x3, "invLightMatrices", RenderView.MAX_FORWARD_LIGHTS),
                        Variable(GLSLType.V4F, "lightData0", RenderView.MAX_FORWARD_LIGHTS),
                        Variable(GLSLType.V1F, "lightData1", RenderView.MAX_FORWARD_LIGHTS),
                        Variable(GLSLType.V4F, "shadowData", RenderView.MAX_FORWARD_LIGHTS),
                        Variable(GLSLType.SCube, "skybox"),
                        // light maps for shadows
                        // - spotlights, directional lights
                        Variable(GLSLType.S2DShadow, "shadowMapPlanar", MAX_PLANAR_LIGHTS),
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
                        Variable(GLSLType.V1F, "finalRoughness"),
                        Variable(GLSLType.V1F, "finalOcclusion"),
                        Variable(GLSLType.V1F, "finalSheen"),
                        // Variable(GLSLType.V3F, "finalSheenNormal"),
                        // Variable(GLSLType.V4F, "finalClearCoat"),
                        // Variable(GLSLType.V2F, "finalClearCoatRoughMetallic"),
                        // if the translucency > 0, the normal map probably should be turned into occlusion ->
                        // no, or at max slightly, because the surrounding area will illuminate it
                        Variable(GLSLType.V1F, "finalTranslucency"),
                        Variable(GLSLType.V1F, "finalAlpha"),
                        Variable(GLSLType.V3F, "finalPosition"),
                        Variable(GLSLType.V3F, "finalNormal"),
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
                            lightCode +
                            combineLightCode +
                            (if (flags.hasFlag(IS_DEFERRED)) "" else skyMapCode) +
                            "#endif\n" +
                            colorToSRGB +
                            "   if(applyToneMapping) finalColor = tonemap(finalColor);\n" +
                            "   finalResult = vec4(finalColor, finalAlpha);\n"
                )
                    .add(randomGLSL)
                    .add(tonemapGLSL)
                    .add(sampleSkyboxForAmbient),
                finalResultStage
            )
        }
    }

    @JvmField
    val frontBackRenderer = SimpleRenderer(
        "front-back", ShaderStage(
            "front-back", listOf(
                Variable(GLSLType.V3F, "finalNormal"),
                Variable(GLSLType.V4F, "finalResult", VariableMode.OUT),
            ), "finalResult = vec4(" +
                    "   (gl_FrontFacing ? vec3(0.0,0.3,1.0) : vec3(1.0,0.0,0.0)) * " +
                    "   (finalNormal.x * 0.4 + 0.6), 1.0);\n" // some simple shading
        )
    )

    // pbr rendering with a few fake lights (which have no falloff)
    @JvmField
    val previewRenderer = object : Renderer("preview") {

        val previewLights = listOf(
            // direction, strength
            Vector4f(-.5f, +1f, .5f, 5f),
            Vector4f(1f, 1f, 0f, 2f),
            Vector4f(0f, 0f, 1f, 1f)
        )

        val tmpDefaultUniforms = ByteBufferPool
            .allocateDirect(previewLights.size * 4 * 4)
            .asFloatBuffer()

        override fun uploadDefaultUniforms(shader: Shader) {
            super.uploadDefaultUniforms(shader)
            GFX.check()
            shader.use()
            val uniform = shader["lightData"]
            if (uniform >= 0) {
                tmpDefaultUniforms.position(0)
                for (data in previewLights) {
                    val f = length(data.x, data.y, data.z)
                    tmpDefaultUniforms.put(data.x / f)
                    tmpDefaultUniforms.put(data.y / f)
                    tmpDefaultUniforms.put(data.z / f)
                    tmpDefaultUniforms.put(data.w)
                }
                tmpDefaultUniforms.flip()
                shader.v4Array(uniform, tmpDefaultUniforms)
                GFX.check()
            }
        }

        override fun getPostProcessing(flags: Int): List<ShaderStage> {
            return listOf(
                ShaderStage(
                    "previewRenderer", listOf(
                        Variable(GLSLType.V4F, "lightData", previewLights.size),
                        Variable(GLSLType.V3F, "finalColor", VariableMode.INOUT),
                        Variable(GLSLType.V1F, "finalAlpha"),
                        Variable(GLSLType.V3F, "finalPosition"),
                        Variable(GLSLType.V1F, "finalRoughness", VariableMode.INOUT),
                        Variable(GLSLType.V1F, "finalMetallic", VariableMode.INOUT),
                        Variable(GLSLType.V1F, "finalSheen"),
                        Variable(GLSLType.V3F, "finalSheenNormal"),
                        Variable(GLSLType.V4F, "finalClearCoat"),
                        Variable(GLSLType.V2F, "finalClearCoatRoughMetallic"),
                        Variable(GLSLType.V3F, "finalNormal"),
                        Variable(GLSLType.V3F, "finalEmissive", VariableMode.INOUT),
                        Variable(GLSLType.V1F, "finalOcclusion"),
                        Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
                    ), "" +
                            // shared pbr data
                            "vec3 V = normalize(-finalPosition);\n" +
                            // light calculations
                            "float NdotV = abs(dot(finalNormal,V));\n" +
                            // precalculate sheen
                            "float sheenFresnel = 1.0 - abs(dot(finalSheenNormal,V));\n" +
                            "float sheen = finalSheen * pow(sheenFresnel, 3.0);\n" +
                            // light calculation
                            "vec3 ambientLight = vec3(0.2);\n" +
                            "vec3 diffuseLight = ambientLight, specularLight = vec3(0.0);\n" +
                            "vec3 diffuseColor  = finalColor * (1.0 - finalMetallic);\n" +
                            "vec3 specularColor = finalColor * finalMetallic;\n" +
                            "bool hasSpecular = dot(specularColor, vec3(1.0)) > 0.0;\n" +
                            specularBRDFv2NoDivInlined2Start +
                            "for(int i=0;i<${previewLights.size};i++){\n" +
                            "   vec4 data = lightData[i];\n" +
                            "   vec3 lightDirection = data.xyz, lightColor = vec3(data.w);\n" +
                            "   float NdotL = dot(finalNormal, lightDirection);\n" +
                            "   if(NdotL > 0.0){\n" +
                            "       if(hasSpecular) {\n" +
                            "           vec3 H = normalize(V + lightDirection);\n" +
                            specularBRDFv2NoDivInlined2 +
                            "           specularLight += lightColor * computeSpecularBRDF;\n" +
                            "       }\n" +
                            "       diffuseLight += lightColor * NdotL;\n" +
                            "   }\n" +
                            "}\n" +
                            specularBRDFv2NoDivInlined2End +
                            colorToLinear +
                            "finalColor = diffuseColor * diffuseLight + specularLight;\n" +
                            "finalColor = finalColor * (1.0 - finalOcclusion) + finalEmissive;\n" +
                            // todo linear2srgb before or after tonemap?
                            colorToSRGB +
                            "finalColor = tonemap(finalColor);\n" +
                            // todo SRGB textures/framebuffers?
                            "finalResult = vec4(finalColor, finalAlpha);\n"
                ).add(randomGLSL).add(tonemapGLSL), finalResultStage
            )
        }
    }

    @JvmField
    val simpleNormalRenderer = object : Renderer("simple-color") {
        override fun getPostProcessing(flags: Int): List<ShaderStage> {
            return listOf(
                ShaderStage(
                    "uiRenderer",
                    listOf(
                        Variable(GLSLType.V3F, "finalColor", VariableMode.INOUT),
                        Variable(GLSLType.V1F, "finalAlpha"),
                        Variable(GLSLType.V3F, "finalNormal"),
                        Variable(GLSLType.V3F, "finalEmissive", VariableMode.INOUT),
                        Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
                    ), "" +
                            colorToLinear +
                            // must not be normalized, or be careful to not divide by zero!
                            "finalColor = (finalColor * (0.6 - 0.4 * finalNormal.x)) + finalEmissive;\n" +
                            colorToSRGB +
                            "finalResult = vec4(finalColor, finalAlpha);\n"
                ), finalResultStage
            )
        }
    }

    @JvmField
    val boneIndicesRenderer = SimpleRenderer("bone-indices", ShaderStage("bi", listOf(
        Variable(GLSLType.V4I, "boneIndices"),
        Variable(GLSLType.V4F, "boneWeights"),
        Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
    ), "" +
            "finalResult =\n" +
            "boneIdToColor(boneIndices.x) * boneWeights.x +\n" +
            "boneIdToColor(boneIndices.y) * boneWeights.y +\n" +
            "boneIdToColor(boneIndices.z) * boneWeights.z +\n" +
            "boneIdToColor(boneIndices.w) * boneWeights.w;\n")
        .add("vec4 boneIdToColor(int index) {\n" + // there are max 256 bones, soo...
                "   float base = sqrt(float(1+((index>>4)&15)) / 16.0);\n" +
                "   float base1 = base * 0.33;\n" +
                "   float g = float((index>>0)&3) * base1;\n" +
                "   float b = float((index>>2)&3) * base1;\n" +
                "   return vec4(base, base-g, base-b, 1.0);\n" +
                "}\n")
    )

    @JvmField
    val boneWeightsRenderer = SimpleRenderer("bone-weights", ShaderStage("bw",
        listOf(
            Variable(GLSLType.V4F, "boneWeights"),
            Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
        ), "finalResult = vec4(boneWeights.xyz, 1.0);\n"))

    @JvmField
    val attributeRenderers = LazyMap({ type: DeferredLayerType ->
        val variables = listOf(
            Variable(GLSLType.floats[type.workDims - 1], type.glslName, VariableMode.IN),
            Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
        )
        val shaderCode = when (type) {
            DeferredLayerType.MOTION -> "" +
                    "finalResult = vec4(${type.glslName}, 1.0);" +
                    "finalResult.rgb *= 10.0;\n" +
                    "finalResult.rgb *= 1.0 / (1.0 + abs(finalResult.rgb));\n" +
                    "finalResult.rgb += 0.5;\n"
            DeferredLayerType.NORMAL, DeferredLayerType.TANGENT, DeferredLayerType.BITANGENT ->
                "finalResult = vec4(${type.glslName}*0.5+0.5, 1.0);\n"
            DeferredLayerType.DEPTH ->
                "finalResult = vec4(vec3(fract(log2(max(gl_FragCoord.z, 1e-36)))),1.0);\n"
            else -> {
                val prefix = if (type == DeferredLayerType.COLOR || type == DeferredLayerType.EMISSIVE) colorToSRGB
                else ""
                prefix + "finalResult = ${
                    when (type.workDims) {
                        1 -> "vec4(vec3(${type.glslName}),1.0)"
                        2 -> "vec4(${type.glslName},0.0,1.0)"
                        3 -> "vec4(${type.glslName},1.0)"
                        4 -> type.glslName
                        else -> ""
                    }
                };\n" + if (type.highDynamicRange) {
                    val name = type.glslName
                    "finalResult.rgb /= 1.0 + max(max(abs($name).x,abs($name).y),abs($name).z);\n"
                } else ""
            }
        }
        val name = type.name
        val stage = ShaderStage(name, variables, shaderCode).add(octNormalPacking)
        SimpleRenderer(name, stage)
    }, DeferredLayerType.values.size)

    @JvmField
    val rawAttributeRenderers = LazyMap({ type: DeferredLayerType ->
        SimpleRenderer(type.name, DeferredSettings(listOf(type)), emptyList())
    }, DeferredLayerType.values.size)

    @JvmField
    val attributeEffects: Map<Pair<DeferredLayerType, DeferredSettings>, Shader?> =
        LazyMap({ (type, settings) ->
            val layer = settings.findLayer(type)
            if (layer != null) {
                val type2 = GLSLType.floats[type.workDims - 1].glslName
                Shader(
                    type.name, coordsList, coordsUVVertexShader, uvList, listOf(
                        Variable(GLSLType.S2D, "source"),
                        Variable(GLSLType.V4F, "result", VariableMode.OUT)
                    ) + depthVars, "" +
                            octNormalPacking +
                            rawToDepth +
                            "void main(){\n" +
                            "   $type2 data = ${type.dataToWork}(texture(source,uv).${layer.mapping});\n" +
                            "   vec3 color = " +
                            when (type.workDims) {
                                1 -> "vec3(data)"
                                2 -> "vec3(data,0.0)"
                                3 -> "data"
                                else -> "data.rgb;\n"
                            } + ";\n" +
                            (if (type.highDynamicRange) {
                                "color /= (1.0+abs(color));\n"
                            } else "") +
                            (if (type == DeferredLayerType.MOTION) {
                                "color += 0.5;\n"
                            } else "") +
                            "   result = vec4(color, 1.0);\n" +
                            "}"
                )
            } else null
        }, DeferredLayerType.values.size)

    @JvmField
    var MAX_PLANAR_LIGHTS = max(GFX.maxBoundTextures / 4, 1)

    @JvmField
    var MAX_CUBEMAP_LIGHTS = max(GFX.maxBoundTextures / 4, 1)
}