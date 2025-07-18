package me.anno.engine.ui.render

import me.anno.ecs.components.light.sky.shaders.FixedSkyShader.fixedSkyCode
import me.anno.ecs.components.light.sky.shaders.SkyShader.Companion.funcFBM
import me.anno.ecs.components.light.sky.shaders.SkyShader.Companion.funcHash
import me.anno.ecs.components.light.sky.shaders.SkyShader.Companion.funcNoise
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToLinear
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToSRGB
import me.anno.engine.ui.render.RendererLib.combineLightCode
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.engine.ui.render.RendererLib.lightCode
import me.anno.engine.ui.render.RendererLib.sampleSkyboxForAmbient
import me.anno.engine.ui.render.RendererLib.skyMapCode
import me.anno.gpu.GFX
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.deferred.PBRLibraryGLTF.specularBRDFv2NoDivInlined2End
import me.anno.gpu.deferred.PBRLibraryGLTF.specularBRDFv2NoDivInlined2Start
import me.anno.gpu.shader.BaseShader.Companion.DRAWING_SKY
import me.anno.gpu.shader.BaseShader.Companion.IS_DEFERRED
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib.randomGLSL
import me.anno.gpu.shader.ShaderLib.brightness
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.gamma
import me.anno.gpu.shader.ShaderLib.gammaInv
import me.anno.gpu.shader.ShaderLib.octNormalPacking
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.shader.renderer.SimpleRenderer
import me.anno.language.translation.NameDesc
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.wrap
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Strings.iff
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
            "   color = clamp(color,vec3(0.0),vec3(1e38));\n" +
            "   float maxTerm = max(max(color.r, color.g), color.b);\n" +
            "   float whitening = 1.0/(1.0 + maxTerm * 0.01);\n" +
            "   color = mix(vec3(1.0), color / (1.0 + maxTerm), whitening);\n" +
            "   return color;\n" +
            "}\n" +
            "vec3 tonemap(vec3 color){\n" +
            "   color = pow(color,vec3($gamma));\n" +
            "   color = tonemapLinear(color);\n" +
            "   color = pow(color,vec3($gammaInv));\n" +
            "   return color;\n" +
            "}\n" +
            "vec4 tonemap(vec4 color){ return vec4(tonemap(color.rgb), color.a); }\n"

    @JvmField
    var tonemapKt = { color: Vector3f ->
        color.div(1f + max(color.max(), 0f))
    }

    @JvmField
    var tonemapInvKt = { color: Vector3f ->
        color.div(1f - max(color.max(), 0f))
    }

    @JvmStatic
    @Suppress("unused")
    fun tonemapKt(color: Vector4f): Vector4f {
        val tmp = JomlPools.vec3f.create()
        tmp.set(color.x, color.y, color.z)
        tonemapKt(tmp)
        color.set(tmp.x, tmp.y, tmp.z)
        JomlPools.vec3f.sub(1)
        return color
    }

    @JvmStatic
    @Suppress("unused")
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
            "finalOverdraw = vec4(0.2);\n"
        )
    )

    @JvmField
    val triangleSizeRenderer = object : Renderer(
        "shadingEfficiency",
    ) {
        override fun getVertexPostProcessing(flags: Int): List<ShaderStage> {
            return ShaderStage(
                "uvw", listOf(Variable(GLSLType.V3F, "uvw", VariableMode.OUT)),
                // todo this may not work with indexed geometry :/ -> a geometry shader could fix that
                // todo debug mode to show indexed geometry
                "uvw = vec3(gl_VertexID % 3 == 0, gl_VertexID % 3 == 1, gl_VertexID % 3 == 2);\n"
            ).wrap()
        }

        override fun getPixelPostProcessing(flags: Int): List<ShaderStage> {
            return ShaderStage(
                "overdraw", listOf(
                    Variable(GLSLType.V3F, "uvw"),
                    Variable(GLSLType.V4F, "finalOverdraw", VariableMode.OUT)
                ),
                "" +
                        "int usage = 0;\n" +
                        "vec3 dx = dFdx(uvw), dy = dFdy(uvw);\n" +
                        // using a 3x3 field instead of a 2x2 field is nicer to look at and shows approx. the same data
                        // "int x0 = -(int(gl_FragCoord.x) & 1);\n" +
                        // "int y0 = -(int(gl_FragCoord.y) & 1);\n" +
                        "for(int yi=-1;yi<=1;yi++){\n" +
                        "   for(int xi=-1;xi<=1;xi++){\n" +
                        "       vec3 u = uvw + dx*float(xi) + dy*float(yi);\n" +
                        "       usage += u.x >= 0.0 && u.y >= 0.0 && u.z >= 0.0 &&" +
                        "           u.x <= 1.0 && u.y <= 1.0 && u.z <= 1.0 ? 1 : 0;\n" +
                        "   }\n" +
                        "}\n" +
                        // "finalOverdraw = vec4(usage < 2 ? 0.5 : 0.0, 0.1, usage == 2 ? 1.0 : 0.0, 1.0);\n" // 2x2
                        "finalOverdraw = vec4(usage <= 2 ? usage <= 1 ? 0.5 : 0.25 : 0.0, 0.1, usage >= 2 && usage <= 5 ? 0.7 : 0.0, 1.0);\n" // 3x3
            ).wrap()
        }
    }

    @JvmField
    val pbrRenderer = object : Renderer(
        "pbr", DeferredSettings(
            listOf(DeferredLayerType.COLOR, DeferredLayerType.ALPHA) +
                    (if (GFX.supportsDepthTextures) emptyList() else listOf(DeferredLayerType.DEPTH))
        )
    ) {
        override fun getPixelPostProcessing(flags: Int): List<ShaderStage> {
            return listOf(
                ShaderStage(
                    "pbr", listOf(
                        // rendering
                        Variable(GLSLType.V1B, "applyToneMapping"),
                        // light data
                        Variable(GLSLType.V1I, "numberOfLights"),
                        Variable(GLSLType.V1B, "receiveShadows"),
                        Variable(GLSLType.V1B, "canHaveShadows"),
                        Variable(GLSLType.M4x3, "invLightMatrices", RenderView.MAX_FORWARD_LIGHTS),
                        Variable(GLSLType.V4F, "lightData0", RenderView.MAX_FORWARD_LIGHTS),
                        Variable(GLSLType.V4F, "lightData1", RenderView.MAX_FORWARD_LIGHTS),
                        Variable(GLSLType.V4F, "lightData2", RenderView.MAX_FORWARD_LIGHTS),
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
                        // Variable(GLSLType.V3F, "finalSheenNormal"),
                        // Variable(GLSLType.V4F, "finalClearCoat"),
                        // Variable(GLSLType.V2F, "finalClearCoatRoughMetallic"),
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
                            lightCode +
                            combineLightCode +
                            (if (flags.hasFlag(IS_DEFERRED)) "" else skyMapCode) +
                            "#endif\n" +
                            colorToLinear +
                            "   if(applyToneMapping) finalColor = tonemapLinear(finalColor);\n" +
                            colorToSRGB +
                            "   finalResult = vec4(finalColor, finalAlpha);\n"
                ).add(randomGLSL).add(tonemapGLSL).add(getReflectivity).add(sampleSkyboxForAmbient)
                    .add(brightness),
                finalResultStage
            )
        }
    }

    @JvmField
    val pbrRendererNoDepth = Renderer.SplitRenderer(
        NameDesc("pbr-nd"), DeferredSettings(listOf(DeferredLayerType.COLOR, DeferredLayerType.ALPHA)),
        pbrRenderer
    )

    @JvmField
    val frontBackRenderer = SimpleRenderer(
        "front-back", ShaderStage(
            "front-back", listOf(
                Variable(GLSLType.V3F, "finalNormal"),
                Variable(GLSLType.V4F, "finalResult", VariableMode.OUT),
            ), "" +
                    "#ifdef SKY\n" +
                    "   finalResult = vec4(0.7, 0.7, 0.7, 1.0);\n" +
                    "#else\n" +
                    "   finalResult = vec4(" +
                    "       (gl_FrontFacing ? vec3(0.0,0.3,1.0) : vec3(1.0,0.0,0.0)) * " +
                    "       (finalNormal.x * 0.4 + 0.6), 1.0);\n" + // some simple shading
                    "#endif\n"
        )
    )

    // pbr rendering with a few fake lights (which have no falloff)
    @JvmField
    val previewRenderer = object : Renderer("preview") {

        override fun getPixelPostProcessing(flags: Int): List<ShaderStage> {
            return listOf(
                ShaderStage(
                    "previewRenderer", listOf(
                        Variable(GLSLType.V3F, "finalColor", VariableMode.INMOD),
                        Variable(GLSLType.V1F, "finalAlpha"),
                        Variable(GLSLType.V3F, "finalPosition"),
                        Variable(GLSLType.V1F, "finalReflectivity"),
                        Variable(GLSLType.V1F, "finalSheen"),
                        Variable(GLSLType.V3F, "finalSheenNormal"),
                        Variable(GLSLType.V4F, "finalClearCoat"),
                        Variable(GLSLType.V2F, "finalClearCoatRoughMetallic"),
                        Variable(GLSLType.V3F, "finalNormal"),
                        Variable(GLSLType.V3F, "finalEmissive"),
                        Variable(GLSLType.V1F, "finalOcclusion"),
                        Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
                    ), "" +
                            colorToLinear +

                            // shared pbr data
                            "vec3 V = normalize(-finalPosition);\n" +

                            // light calculations
                            "float NdotV = abs(dot(finalNormal,V));\n" +

                            // precalculate sheen
                            "float sheenFresnel = 1.0 - abs(dot(finalSheenNormal,V));\n" +
                            "float sheen = finalSheen * pow(sheenFresnel, 3.0);\n" +

                            // light calculation
                            // model ambient light using simple sky model
                            "float reflectivity = finalReflectivity;\n" +
                            "vec3 diffuseColor  = finalColor * (1.0-reflectivity);\n" +
                            "vec3 specularColor = finalColor * reflectivity;\n" +
                            "bool hasSpecular = dot(specularColor, vec3(1.0)) > 0.0;\n" +
                            "vec3 baseAmbient = vec3(exp(dot(finalNormal,vec3(0.4,0.7,0.2))) * 3.0);\n" +

                            "#ifndef DRAWING_SKY\n" +
                            "   vec3 ambientLight = hasSpecular ? getSkyColor(finalNormal) : baseAmbient;\n" +
                            "#else\n" +
                            "   vec3 ambientLight = baseAmbient;\n" +
                            "#endif\n" +

                            "ambientLight = mix(baseAmbient, ambientLight, reflectivity);\n" +
                            "vec3 diffuseLight = ambientLight, specularLight = ambientLight;\n" +
                            "if (hasSpecular) {\n" +
                            "   vec3 reflectedV = -reflect(V,finalNormal);\n" +
                            "   specularLight = mix(specularLight, getSkyColor(reflectedV), reflectivity);\n" +
                            "}\n" +

                            specularBRDFv2NoDivInlined2Start +
                            specularBRDFv2NoDivInlined2End +
                            "finalColor = diffuseColor * diffuseLight + specularLight * specularColor;\n" +
                            "finalColor = finalColor * (1.0 - finalOcclusion) + finalEmissive;\n" +
                            "finalColor = tonemapLinear(finalColor);\n" +
                            colorToSRGB +
                            "finalResult = vec4(finalColor, finalAlpha);\n"
                ).add(randomGLSL).add(tonemapGLSL).add(getReflectivity).apply {
                    if (!flags.hasFlag(DRAWING_SKY)) {
                        add(fixedSkyCode).add(funcHash).add(funcNoise).add(funcFBM)
                    }
                }, finalResultStage
            )
        }
    }

    @JvmField
    val simpleRenderer = object : Renderer("simple-color") {
        override fun getPixelPostProcessing(flags: Int): List<ShaderStage> {
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
                            colorToSRGB + // accuracy doesn't matter here
                            // must not be normalized, or be careful to not divide by zero!
                            "float lightFactor = pow(0.5 + 0.5 * dot(finalNormal,vec3(-0.74,0.6,0.3)), 6.0);\n" +
                            "finalColor = 40.0 * (finalColor * mix(vec3(0.017,0.021,0.03),vec3(1.0),lightFactor)) + 2.5 * finalEmissive;\n" +
                            "finalColor *= 1.0 / (1.0 + max(finalColor.x,max(finalColor.y,finalColor.z)));\n" +
                            "finalResult = vec4(finalColor, finalAlpha);\n"
                ), finalResultStage
            )
        }
    }

    @JvmField
    val isInstancedRenderer = object : Renderer("isInstanced") {
        override fun getPixelPostProcessing(flags: Int): List<ShaderStage> {
            return listOf(
                ShaderStage(
                    "isInstanced", listOf(
                        Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
                    ), "" +
                            "float f;\n" +
                            "#ifdef INSTANCED\n" +
                            "   f = 1.0;\n" +
                            "#elif defined(SKY)\n" +
                            "   f = 0.5;\n" +
                            "#else\n" +
                            "   f = 0.0;\n" +
                            "#endif\n" +
                            "finalResult = vec4(f,f,f,1.0);\n"
                )
            )
        }
    }

    @JvmField
    val isIndexedRenderer = object : Renderer("isIndexed") {
        override fun getPixelPostProcessing(flags: Int): List<ShaderStage> {
            return listOf(
                ShaderStage(
                    "isIndexed", listOf(
                        Variable(GLSLType.V1B, "isIndexed"),
                        Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
                    ), "" +
                            "float f;\n" +
                            "#ifdef SKY\n" +
                            "   f = 0.5;\n" +
                            "#else\n" +
                            "   f = isIndexed ? 1.0 : 0.0;\n" +
                            "#endif\n" +
                            "finalResult = vec4(f,f,f,1.0);\n"
                )
            )
        }
    }

    @JvmField
    val diffFromNormalRenderer = object : Renderer("diffFromNormal") {
        override fun getPixelPostProcessing(flags: Int): List<ShaderStage> {
            return listOf(
                ShaderStage(
                    "curvature", listOf(
                        Variable(GLSLType.V3F, "finalPosition"),
                        Variable(GLSLType.V3F, "finalNormal"),
                        Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
                    ), "" +
                            "#ifdef SKY\n" +
                            "   finalResult = vec4(0.0,0.0,0.0,1.0);\n" +
                            "#else\n" +
                            "   vec3 theoNormal = normalize(cross(dFdx(finalPosition),dFdy(finalPosition)));\n" +
                            "   float f = abs(dot(theoNormal,finalNormal)/max(length(finalNormal),1e-38));\n" +
                            "   f = 1.0-pow(f,4.0);\n" + // transform to see stuff easier
                            "   finalResult = vec4(f,f,f,1.0);\n" +
                            "#endif\n"
                )
            )
        }
    }

    @JvmField
    val normalMapRenderer = object : Renderer("showNormalMap") {
        override fun getPixelPostProcessing(flags: Int): List<ShaderStage> {
            return listOf(
                ShaderStage(
                    "showNormalMap", listOf(
                        Variable(GLSLType.V2F, "uv"),
                        Variable(GLSLType.S2D, "normalMap"),
                        Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
                    ), "finalResult = vec4(texture(normalMap,uv).rgb,1.0);\n"
                )
            )
        }
    }

    @JvmField
    val boneIndicesRenderer = object : Renderer("bone-indices") {
        override fun getVertexPostProcessing(flags: Int): List<ShaderStage> {
            return listOf(
                ShaderStage(
                    "bif", listOf(
                        Variable(GLSLType.V4I, "boneIndices"),
                        Variable(GLSLType.V4F, "boneWeights"),
                        Variable(GLSLType.V4F, "boneColor", VariableMode.OUT)
                    ), "" +
                            "boneColor =\n" +
                            "boneIdToColor(boneIndices.x) * boneWeights.x +\n" +
                            "boneIdToColor(boneIndices.y) * boneWeights.y +\n" +
                            "boneIdToColor(boneIndices.z) * boneWeights.z +\n" +
                            "boneIdToColor(boneIndices.w) * boneWeights.w;\n"
                )
                    .add(
                        "vec4 boneIdToColor(int index) {\n" + // there are max 256 bones, soo...
                                "   float base = sqrt(float(1+((index>>4)&15)) / 16.0);\n" +
                                "   float base1 = base * 0.33;\n" +
                                "   float g = float((index>>0)&3) * base1;\n" +
                                "   float b = float((index>>2)&3) * base1;\n" +
                                "   return vec4(base, base-g, base-b, 1.0);\n" +
                                "}\n"
                    )
            )
        }

        override fun getPixelPostProcessing(flags: Int): List<ShaderStage> {
            return listOf(
                ShaderStage(
                    "biv", listOf(
                        Variable(GLSLType.V4F, "boneColor"),
                        Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
                    ), "finalResult = boneColor;\n"
                )
            )
        }
    }

    @JvmField
    val boneWeightsRenderer = SimpleRenderer(
        "bone-weights", ShaderStage(
            "bw",
            listOf(
                Variable(GLSLType.V4F, "boneWeights"),
                Variable(GLSLType.V4F, "finalResult", VariableMode.OUT)
            ), "finalResult = vec4(boneWeights.xyz, 1.0);\n"
        )
    )

    @JvmField
    val attributeRenderers = LazyMap({ type: DeferredLayerType ->
        val variables = listOf(
            Variable(GLSLType.floats[type.workDims - 1], type.glslName, VariableMode.IN),
            Variable(GLSLType.V1B, "reverseDepth"),
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
            DeferredLayerType.DEPTH -> "" +
                    "float depth = gl_FragCoord.z;\n" +
                    "#ifdef CUSTOM_DEPTH\n" +
                    "   depth = gl_FragDepth;\n" +
                    "#endif\n" +
                    "float depth1 = reverseDepth ? depth : 1.0 - depth;\n" +
                    "float color = fract(log2(max(depth1, reverseDepth ? 1.0e-36 : 0.8e-7)));\n" +
                    "finalResult = vec4(vec3(color),1.0);\n"
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
                    type.name, emptyList(), coordsUVVertexShader, uvList, listOf(
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
                            "color /= (1.0+abs(color));\n".iff(type.highDynamicRange) +
                            "color += 0.5;\n".iff(type == DeferredLayerType.MOTION) +
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