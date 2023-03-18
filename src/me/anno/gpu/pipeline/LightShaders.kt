package me.anno.gpu.pipeline

import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.LightType
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.light.SpotLight
import me.anno.engine.pbr.PBRLibraryGLTF
import me.anno.engine.ui.render.Renderers
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.IFramebuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureLib
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector3f
import org.lwjgl.opengl.GL15C

object LightShaders {

    private val lightInstancedAttributes = listOf(
        // transform
        Attribute("instanceTrans0", 3),
        Attribute("instanceTrans1", 3),
        Attribute("instanceTrans2", 3),
        Attribute("instanceTrans3", 3),
        // inverse transform for light mapping
        Attribute("invInsTrans0", 3),
        Attribute("invInsTrans1", 3),
        Attribute("invInsTrans2", 3),
        Attribute("invInsTrans3", 3),
        // light properties like type, color, cone angle
        Attribute("lightData0", 4),
        Attribute("lightData1", 4),
        Attribute("shadowData", 4)
        // instanced rendering does not support shadows -> no shadow data / as a uniform
    )

    private val lightCountInstancedAttributes = listOf(
        // transform
        Attribute("instanceTrans0", 3),
        Attribute("instanceTrans1", 3),
        Attribute("instanceTrans2", 3),
        Attribute("instanceTrans3", 3),
        Attribute("shadowData", 1)
        // instanced rendering does not support shadows -> no shadow data / as a uniform
    )

    // test: can we get better performance, when we eliminate dependencies?
    // pro: fewer dependencies
    // con: we use more vram
    // con: we remove cache locality
    /*private val lightInstanceBuffers = Array(512) {
        StaticBuffer(lightInstancedAttributes, instancedBatchSize, GL_DYNAMIC_DRAW)
    }
    private var libIndex = 0
    // performance: the same
    */

    val lightInstanceBuffer =
        StaticBuffer(lightInstancedAttributes, PipelineStage.instancedBatchSize, GL15C.GL_DYNAMIC_DRAW)
    val lightCountInstanceBuffer =
        StaticBuffer(lightCountInstancedAttributes, PipelineStage.instancedBatchSize, GL15C.GL_DYNAMIC_DRAW)

    val useMSAA get() = GFXState.currentBuffer.samples > 1

    fun combineLighting(
        deferred: DeferredSettingsV2, applyToneMapping: Boolean, ambientLight: Vector3f,
        scene: IFramebuffer, light: IFramebuffer, ssao: ITexture2D,
    ) {
        val shader = getPostShader(deferred)
        shader.use()
        shader.v1b("applyToneMapping", applyToneMapping)
        shader.v3f("ambientLight", ambientLight)
        scene.bindTrulyNearestMS(2)
        ssao.bindTrulyNearest(1)
        light.bindTrulyNearestMS(0)
        GFX.flat01.draw(shader)
    }

    fun bindNullDepthTextures(shader: Shader) {
        for (i in 0 until 8) {
            val tx = shader.getTextureIndex("shadowMapPlanar$i")
            if (tx < 0) break
            TextureLib.depthTexture.bindTrulyNearest(tx)
        }
        for (i in 0 until 8) {
            val tx = shader.getTextureIndex("shadowMapCubic$i")
            if (tx < 0) break
            TextureLib.depthCube.bindTrulyNearest(tx)
        }
    }

    fun getPostShader(settingsV2: DeferredSettingsV2): Shader {
        val useMSAA = useMSAA
        val code = if (useMSAA) -1 else -2
        return shaderCache.getOrPut(settingsV2 to code) {
            /*
            * vec3 diffuseColor  = finalColor * (1.0 - finalMetallic);
            * vec3 specularColor = finalColor * finalMetallic;
            * finalColor = diffuseColor * diffuseLight + specularLight; // specular already contains the color
            * finalColor = finalColor * (1.0 - finalOcclusion) + finalEmissive; // color, happens in post-processing
            * finalColor = tonemap(finalColor); // tone mapping
            * */
            val builder = ShaderBuilder("post")
            builder.addVertex(
                ShaderStage(
                    "v", listOf(
                        Variable(GLSLType.V2F, "coords", VariableMode.ATTR),
                        Variable(GLSLType.V2F, "uv", VariableMode.OUT)
                    ), "gl_Position = vec4(coords*2.0-1.0,0.5,1.0);\n" +
                            "uv = coords;\n"
                )
            )

            val fragment = ShaderStage(
                "f", listOf(
                    Variable(GLSLType.V3F, "finalColor"),
                    Variable(GLSLType.V3F, "finalPosition"),
                    Variable(GLSLType.V1F, "finalOcclusion"),
                    Variable(GLSLType.V3F, "finalEmissive"),
                    Variable(GLSLType.V1B, "applyToneMapping"),
                    Variable(GLSLType.S2D, "finalLight"),
                    Variable(GLSLType.S2D, "ambientOcclusion"),
                    Variable(GLSLType.V3F, "ambientLight"),
                    Variable(GLSLType.V4F, "color", VariableMode.OUT)
                ), "" +
                        "   vec3 color3;\n" +
                        "   if(length(finalPosition) < 1e34) {\n" +
                        "       vec3 light = texture(finalLight, uv).rgb + ambientLight;\n" +
                        "       float occlusion = (1.0 - finalOcclusion) * (1.0 - texture(ambientOcclusion, uv).r);\n" +
                        "       color3 = finalColor * light * occlusion + finalEmissive;\n" +
                        "   } else color3 = finalColor + finalEmissive;\n" + // sky
                        "   if(applyToneMapping) color3 = tonemap(color3);\n" +
                        "   color = vec4(color3, 1.0);\n"
            )
            fragment.add(Renderers.tonemapGLSL)

            // deferred inputs
            // find deferred layers, which exist, and appear in the shader
            val deferredCode = StringBuilder()
            val deferredInputs = ArrayList<Variable>()
            deferredInputs += Variable(GLSLType.V2F, "uv")
            val imported = HashSet<String>()
            val sampleVariableName = if (useMSAA) "gl_SampleID" else null
            val samplerType = if (useMSAA) GLSLType.S2DMS else GLSLType.S2D
            for (layer in settingsV2.layers) {
                // if this layer is present,
                // then define the output,
                // and write the mapping
                val glslName = layer.type.glslName
                if (fragment.variables.any2 { it.name == glslName }) {
                    layer.appendMapping(deferredCode, "Tmp", "uv", imported, sampleVariableName)
                }
            }
            deferredInputs += imported.map { Variable(samplerType, it, VariableMode.IN) }
            builder.addFragment(ShaderStage("deferred", deferredInputs, deferredCode.toString()))
            builder.addFragment(fragment)
            if (useMSAA) builder.glslVersion = 400 // required for gl_SampleID
            val shader = builder.create()
            // find all textures
            // first the ones for the deferred data
            // then the ones for the shadows
            val textures = listOf("finalLight", "ambientOcclusion") + settingsV2.layers2.map { it.name }
            shader.ignoreNameWarnings(
                "tint", "invLocalTransform",
                "defLayer0", "defLayer1", "defLayer2", "defLayer3",
                "defLayer4", "defLayer5", "defLayer6", "defLayer7"
            )
            shader.setTextureIndices(textures)
            shader
        }
    }

    var countPerPixel = 0.25f

    private val shaderCache = HashMap<Pair<DeferredSettingsV2, Int>, Shader>()
    fun getShader(settingsV2: DeferredSettingsV2, type: LightType): Shader {
        val isInstanced = GFXState.instanced.currentValue
        val useMSAA = useMSAA
        val key = type.ordinal * 4 + useMSAA.toInt(2) + isInstanced.toInt()
        return shaderCache.getOrPut(settingsV2 to key) {

            /*
            * vec3 diffuseColor  = finalColor * (1.0 - finalMetallic);
            * vec3 specularColor = finalColor * finalMetallic;
            * finalColor = diffuseColor * diffuseLight + specularLight; // specular already contains the color
            * finalColor = finalColor * finalOcclusion + finalEmissive; // color, happens in post-processing
            * finalColor = tonemap(finalColor); // tone mapping
            * */
            val builder = ShaderBuilder("$type-$isInstanced")
            val vertexStage = if (isInstanced) {
                ShaderStage(
                    "v", listOf(
                        Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
                        Variable(GLSLType.V3F, "instanceTrans0", VariableMode.ATTR),
                        Variable(GLSLType.V3F, "instanceTrans1", VariableMode.ATTR),
                        Variable(GLSLType.V3F, "instanceTrans2", VariableMode.ATTR),
                        Variable(GLSLType.V3F, "instanceTrans3", VariableMode.ATTR),
                        Variable(GLSLType.V3F, "invInsTrans0", VariableMode.ATTR),
                        Variable(GLSLType.V3F, "invInsTrans1", VariableMode.ATTR),
                        Variable(GLSLType.V3F, "invInsTrans2", VariableMode.ATTR),
                        Variable(GLSLType.V3F, "invInsTrans3", VariableMode.ATTR),
                        Variable(GLSLType.V4F, "lightData0", VariableMode.ATTR),
                        Variable(GLSLType.V4F, "lightData1", VariableMode.ATTR),
                        Variable(GLSLType.V4F, "shadowData", VariableMode.ATTR),
                        Variable(GLSLType.M4x4, "transform", VariableMode.IN),
                        Variable(GLSLType.V4F, "data0", VariableMode.OUT),
                        Variable(GLSLType.V4F, "data1", VariableMode.OUT),
                        Variable(GLSLType.V4F, "data2", VariableMode.OUT),
                        Variable(GLSLType.M4x3, "WStoLightSpace", VariableMode.OUT),
                        Variable(GLSLType.V3F, "uvw", VariableMode.OUT)
                    ), "" +
                            "data0 = lightData0;\n" +
                            "data1 = lightData1;\n" +
                            "data2 = shadowData;\n" +
                            // cutoff = 0 -> scale onto the whole screen, has effect everywhere
                            "if(${type == LightType.DIRECTIONAL} && data2.a <= 0.0){\n" +
                            "   gl_Position = vec4(coords.xy, 0.5, 1.0);\n" +
                            "} else {\n" +
                            "   mat4x3 localTransform = mat4x3(instanceTrans0,instanceTrans1,instanceTrans2,instanceTrans3);\n" +
                            "   gl_Position = transform * vec4(localTransform * vec4(coords, 1.0), 1.0);\n" +
                            "}\n" +
                            "WStoLightSpace = mat4x3(invInsTrans0,invInsTrans1,invInsTrans2,invInsTrans3);\n" +
                            "uvw = gl_Position.xyw;\n"
                )
            } else {
                ShaderStage(
                    "v", listOf(
                        Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
                        Variable(GLSLType.M4x4, "transform", VariableMode.IN),
                        Variable(GLSLType.M4x3, "localTransform", VariableMode.IN),
                        Variable(GLSLType.V1F, "cutoff", VariableMode.IN),
                        Variable(GLSLType.V3F, "uvw", VariableMode.OUT)
                    ), "" +
                            // cutoff = 0 -> scale onto the whole screen, has effect everywhere
                            "if(${type == LightType.DIRECTIONAL} && cutoff <= 0.0){\n" +
                            "   gl_Position = vec4(coords.xy, 0.5, 1.0);\n" +
                            "} else {\n" +
                            "   gl_Position = transform * vec4(localTransform * vec4(coords, 1.0), 1.0);\n" +
                            "}\n" +
                            "uvw = gl_Position.xyw;\n"
                )
            }
            builder.addVertex(vertexStage)
            builder.addFragment(
                ShaderStage(
                    "uv", listOf(
                        Variable(GLSLType.V3F, "uvw", VariableMode.IN),
                        Variable(GLSLType.V2F, "uv", VariableMode.OUT)
                    ), "uv = uvw.xy/uvw.z*.5+.5;\n"
                )
            )
            val withShadows = !isInstanced
            val cutoffContinue = "discard"
            val coreFragment = when (type) {
                LightType.SPOT -> SpotLight.getShaderCode(cutoffContinue, withShadows)
                LightType.DIRECTIONAL -> DirectionalLight.getShaderCode(cutoffContinue, withShadows)
                LightType.POINT -> PointLight.getShaderCode(cutoffContinue, withShadows, true)
            }
            val fragment = ShaderStage(
                "f", listOf(
                    Variable(GLSLType.V4F, "data0"),
                    Variable(GLSLType.V4F, "data1"),
                    Variable(GLSLType.V4F, "data2"), // only if with shadows
                    // light maps for shadows
                    // - spotlights, directional lights
                    Variable(GLSLType.S2DShadow, "shadowMapPlanar", Renderers.MAX_PLANAR_LIGHTS),
                    // - point lights
                    Variable(GLSLType.SCubeShadow, "shadowMapCubic", 1),
                    Variable(GLSLType.V1B, "receiveShadows"),
                    // Variable(GLSLType.V3F, "finalColor"), // not really required
                    Variable(GLSLType.V3F, "finalPosition"),
                    Variable(GLSLType.V3F, "finalColor"),
                    Variable(GLSLType.V3F, "finalNormal"),
                    // Variable(GLSLType.V1F, "finalOcclusion"), post-process, including ambient
                    Variable(GLSLType.V1F, "finalMetallic"),
                    Variable(GLSLType.V1F, "finalRoughness"),
                    Variable(GLSLType.V1F, "finalSheen"),
                    Variable(GLSLType.V1F, "finalTranslucency"),
                    Variable(GLSLType.M4x3, "WStoLightSpace"), // invLightMatrices[i]
                    Variable(GLSLType.V4F, "light", VariableMode.OUT)
                ), "" +
                        // light calculation including shadows if !instanced
                        "vec3 diffuseLight = vec3(0.0), specularLight = vec3(0.0);\n" +
                        "bool hasSpecular = finalMetallic > 0.0;\n" +
                        "vec3 V = normalize(-finalPosition);\n" +
                        "float NdotV = dot(finalNormal,V);\n" +
                        "int shadowMapIdx0 = 0;\n" + // always 0 at the start
                        "int shadowMapIdx1 = int(data2.g);\n" +
                        // light properties, which are typically inside the loop
                        "vec3 lightColor = data0.rgb;\n" +
                        "vec3 dir = WStoLightSpace * vec4(finalPosition, 1.0);\n" +
                        "vec3 localNormal = normalize(mat3x3(WStoLightSpace) * finalNormal);\n" +
                        "float NdotL = 0.0;\n" + // normal dot light
                        "vec3 effectiveDiffuse, effectiveSpecular, lightPosition, lightDirWS = vec3(0.0);\n" +
                        coreFragment +
                        "if(hasSpecular && NdotL > 0.0001 && NdotV > 0.0001){\n" +
                        "   vec3 H = normalize(V + lightDirWS);\n" +
                        PBRLibraryGLTF.specularBRDFv2NoColorStart +
                        PBRLibraryGLTF.specularBRDFv2NoColor +
                        "   specularLight = effectiveSpecular * computeSpecularBRDF;\n" +
                        PBRLibraryGLTF.specularBRDFv2NoColorEnd +
                        "}\n" +
                        // translucency; looks good and approximately correct
                        // sheen is a fresnel effect, which adds light at the edge, e.g., for clothing
                        "NdotL = mix(NdotL, 0.23, finalTranslucency) + finalSheen;\n" +
                        "diffuseLight += effectiveDiffuse * clamp(NdotL, 0.0, 1.0);\n" +
                        // ~65k is the limit, after that only Infinity
                        // todo car sample's light on windows looks clamped... who is clamping it?
                        "vec3 color = mix(diffuseLight, specularLight, finalMetallic);\n" +
                        "light = vec4(clamp(color, 0.0, 16e3), 1.0);\n" +
                        ""
            )

            // deferred inputs
            // find deferred layers, which exist, and appear in the shader
            val deferredCode = StringBuilder()
            val deferredInputs = ArrayList<Variable>()
            deferredInputs += Variable(GLSLType.V2F, "uv")
            val imported = HashSet<String>()
            val sampleVariableName = if (useMSAA) "gl_SampleID" else null
            val samplerType = if (useMSAA) GLSLType.S2DMS else GLSLType.S2D
            for (layer in settingsV2.layers) {
                // if this layer is present,
                // then define the output,
                // and write the mapping
                val glslName = layer.type.glslName
                if (fragment.variables.any2 { it.name == glslName }) {
                    layer.appendMapping(deferredCode, "Tmp", "uv", imported, sampleVariableName)
                }
            }
            deferredInputs += imported.map { Variable(samplerType, it, VariableMode.IN) }
            builder.addFragment(ShaderStage("deferred", deferredInputs, deferredCode.toString()))
            builder.addFragment(fragment)
            if (useMSAA) builder.glslVersion = 400 // required for gl_SampleID
            val shader = builder.create()
            // find all textures
            // first the ones for the deferred data
            // then the ones for the shadows
            val textures = settingsV2.layers2.map { it.name } +
                    listOf("shadowMapCubic0") +
                    Array(Renderers.MAX_PLANAR_LIGHTS) { "shadowMapPlanar$it" }
            shader.ignoreNameWarnings(
                "tint", "invLocalTransform", "colors",
                "tangents", "uvs", "normals", "isDirectional",
                "defLayer0", "defLayer1", "defLayer2", "defLayer3", "defLayer4",
                "receiveShadows", "countPerPixel"
            )
            shader.setTextureIndices(textures)
            shader
        }
    }

    val visualizeLightCountShader = Shader(
        "visualize-light-count",
        listOf(
            Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.M4x3, "localTransform"),
            Variable(GLSLType.V1B, "fullscreen"),
        ), "" +
                "void main(){\n" +
                // cutoff = 0 -> scale onto the whole screen, has effect everywhere
                "   if(fullscreen){\n" +
                "      gl_Position = vec4(coords.xy, 0.5, 1.0);\n" +
                "   } else {\n" +
                "      gl_Position = transform * vec4(localTransform * vec4(coords, 1.0), 1.0);\n" +
                "   }\n" +
                "}\n", emptyList(), listOf(
            Variable(GLSLType.V1F, "countPerPixel"),
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ), "" +
                "void main(){ result = vec4(countPerPixel); }"
    )

    val visualizeLightCountShaderInstanced = Shader(
        "visualize-light-count-instanced", listOf(
            Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
            Variable(GLSLType.V3F, "instanceTrans0", VariableMode.ATTR),
            Variable(GLSLType.V3F, "instanceTrans1", VariableMode.ATTR),
            Variable(GLSLType.V3F, "instanceTrans2", VariableMode.ATTR),
            Variable(GLSLType.V3F, "instanceTrans3", VariableMode.ATTR),
            Variable(GLSLType.V4F, "shadowData", VariableMode.ATTR),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.V1B, "isDirectional"),
        ), "" +
                "void main(){\n" +
                // cutoff = 0 -> scale onto the whole screen, has effect everywhere
                "   if(isDirectional && shadowData.a <= 0.0){\n" +
                "      gl_Position = vec4(coords.xy, 0.5, 1.0);\n" +
                "   } else {\n" +
                "       mat4x3 localTransform = mat4x3(instanceTrans0,instanceTrans1,instanceTrans2,instanceTrans3);\n" +
                "      gl_Position = transform * vec4(localTransform * vec4(coords, 1.0), 1.0);\n" +
                "   }\n" +
                "}", emptyList(), listOf(
            Variable(GLSLType.V1F, "countPerPixel"),
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ), "" +
                "void main(){ result = vec4(countPerPixel); }"
    ).apply {
        ignoreNameWarnings("normals", "uvs", "tangents", "colors", "receiveShadows")
    }
}