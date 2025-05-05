package me.anno.gpu.pipeline

import me.anno.ecs.components.light.LightType
import me.anno.ecs.components.mesh.utils.MeshInstanceData
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToLinear
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToSRGB
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.engine.ui.render.RendererLib.sampleSkyboxForAmbient
import me.anno.engine.ui.render.Renderers.tonemapGLSL
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeLayout.Companion.bind
import me.anno.gpu.buffer.BufferUsage
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.deferred.DeferredSettings
import me.anno.gpu.deferred.PBRLibraryGLTF.specularBRDFv2NoColor
import me.anno.gpu.deferred.PBRLibraryGLTF.specularBRDFv2NoColorStart
import me.anno.gpu.pipeline.InstancedBuffers.instancedBatchSize
import me.anno.gpu.shader.BaseShader.Companion.getKey
import me.anno.gpu.shader.DepthTransforms.bindDepthUniforms
import me.anno.gpu.shader.DepthTransforms.depthToPosition
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.GLSLType.Companion.floats
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.loadMat4x3
import me.anno.gpu.shader.ShaderLib.octNormalPacking
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.ShaderLib.roughnessIfMissing
import me.anno.gpu.shader.builder.ShaderBuilder
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.TextureLib
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.structures.lists.Lists.createList
import me.anno.utils.types.Booleans.toInt

object LightShaders {

    val translucencyNL = 0.5f

    val startLightSum = "" +
            "vec3 diffuseLight = vec3(0.0), specularLight = vec3(0.0);\n" +
            "float reflectivity = finalReflectivity;\n" +
            "bool hasSpecular = reflectivity > 0.0;\n" +
            specularBRDFv2NoColorStart

    val addSpecularLight = "" +
            "if(hasSpecular && NdotL > 0.0001 && NdotV > 0.0001){\n" +
            "   vec3 lightV = normalize(matMul(camSpaceToLightSpace,vec4(V,0.0)));\n" +
            "   vec3 lightH = normalize(lightV + lightDir);\n" +
            specularBRDFv2NoColor +
            "   specularLight += (300.0 * pow(reflectivity,2.0)) * effectiveSpecular * computeSpecularBRDF;\n" +
            "}\n"

    val addDiffuseLight = "" + // translucency; looks good and approximately correct
            // sheen is a fresnel effect, which adds light at the edge, e.g., for clothing
            "float NdotLi = mix(NdotL, $translucencyNL, finalTranslucency) + finalSheen;\n" +
            "diffuseLight += effectiveDiffuse * clamp(NdotLi, 0.0, 1.0);\n"

    val mixAndClampLight = "" +
            "light = mix(diffuseLight, specularLight, reflectivity);\n" +
            // ~65k is the limit, after that only Infinity
            "light = clamp(light, 0.0, 16e3);\n"

    val combineLightFinishLine =
        "   finalColor = finalColor * light * pow(invOcclusion, 2.0) + finalEmissive * mix(1.0, invOcclusion, finalReflectivity);\n"

    private val lightInstancedAttributes = bind(
        // transform
        Attribute("instanceTrans0", 4),
        Attribute("instanceTrans1", 4),
        Attribute("instanceTrans2", 4),
        // inverse transform for light mapping
        Attribute("invInsTrans0", 4),
        Attribute("invInsTrans1", 4),
        Attribute("invInsTrans2", 4),
        // light properties like type, color, cone angle
        Attribute("lightData0", 4),
        Attribute("lightData1", 4),
        // instanced rendering does not support shadows -> no shadow data / as a uniform
    )

    val lightInstanceBuffer = StaticBuffer("lights", lightInstancedAttributes, instancedBatchSize, BufferUsage.STREAM)

    val useMSAA: Boolean get() = GFXState.currentBuffer.samples > 1

    fun combineLighting(shader: Shader, applyToneMapping: Boolean) {
        shader.v1b("applyToneMapping", applyToneMapping)
        bindDepthUniforms(shader)
        flat01.draw(shader)
    }

    private val planarNames = createList(8) { "shadowMapPlanar$it" }
    private val cubicNames = createList(8) { "shadowMapCubic$it" }

    fun bindNullDepthTextures(shader: Shader) {
        val depthTexture =
            if (GFX.supportsDepthTextures) TextureLib.depthTexture
            else TextureLib.blackTexture
        for (i in 0 until 8) {
            val tx = shader.getTextureIndex(planarNames[i])
            if (tx < 0) break
            depthTexture.bindTrulyNearest(tx)
        }
        val depthCube =
            if (GFX.supportsDepthTextures) TextureLib.depthCube
            else TextureLib.blackCube
        for (i in 0 until 8) {
            val tx = shader.getTextureIndex(cubicNames[i])
            if (tx < 0) break
            depthCube.bindTrulyNearest(tx)
        }
    }

    val combineVStage = ShaderStage(
        "combineLight-v",
        coordsList + Variable(GLSLType.V2F, "uv", VariableMode.OUT),
        "gl_Position = vec4(positions*2.0-1.0,0.5,1.0);\nuv = positions;\n"
    )

    val combineFStage = ShaderStage(
        "combineLight-f", listOf(
            Variable(GLSLType.V3F, "finalColor", VariableMode.INMOD),
            Variable(GLSLType.V3F, "finalEmissive", VariableMode.INMOD),
            Variable(GLSLType.SCube, "reflectionMap"),
            Variable(GLSLType.V3F, "finalNormal"),
            Variable(GLSLType.V1F, "finalReflectivity"),
            Variable(GLSLType.V1F, "finalOcclusion"),
            Variable(GLSLType.V1B, "applyToneMapping"),
            Variable(GLSLType.V3F, "finalLight"),
            Variable(GLSLType.V1F, "ambientOcclusion"),
            Variable(GLSLType.V4F, "color", VariableMode.OUT)
        ), "" +
                colorToLinear +
                roughnessIfMissing +
                "   vec3 light = finalLight + sampleSkyboxForAmbient(finalNormal, finalRoughness, finalReflectivity);\n" +
                "   float invOcclusion = (1.0 - finalOcclusion) * (1.0 - ambientOcclusion);\n" +
                combineLightFinishLine +
                "   if(applyToneMapping) finalColor = tonemapLinear(finalColor);\n" +
                colorToSRGB +
                "   color = vec4(finalColor, 1.0);\n"
    ).add(tonemapGLSL).add(getReflectivity).add(sampleSkyboxForAmbient)

    var countPerPixel = 0.25f

    val vertexI = ShaderStage(
        "v", listOf(
            Variable(GLSLType.V3F, "positions", VariableMode.ATTR),
            Variable(GLSLType.V4F, "instanceTrans0", VariableMode.ATTR),
            Variable(GLSLType.V4F, "instanceTrans1", VariableMode.ATTR),
            Variable(GLSLType.V4F, "instanceTrans2", VariableMode.ATTR),
            Variable(GLSLType.V4F, "invInsTrans0", VariableMode.ATTR),
            Variable(GLSLType.V4F, "invInsTrans1", VariableMode.ATTR),
            Variable(GLSLType.V4F, "invInsTrans2", VariableMode.ATTR),
            Variable(GLSLType.V4F, "lightData0", VariableMode.ATTR),
            Variable(GLSLType.V4F, "lightData1", VariableMode.ATTR),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.V1B, "isDirectional"),
            Variable(GLSLType.V1B, "isSpotLight"),
            Variable(GLSLType.V4F, "invInsTrans0v", VariableMode.OUT).flat(),
            Variable(GLSLType.V4F, "invInsTrans1v", VariableMode.OUT).flat(),
            Variable(GLSLType.V4F, "invInsTrans2v", VariableMode.OUT).flat(),
            Variable(GLSLType.V4F, "data0", VariableMode.OUT).flat(),
            Variable(GLSLType.V4F, "data1", VariableMode.OUT).flat(),
            Variable(GLSLType.V4F, "data2", VariableMode.OUT).flat(),
            Variable(GLSLType.V3F, "uvw", VariableMode.OUT),
        ), "" +
                "data0 = lightData0;\n" + // color, type
                "data1 = lightData1;\n" + // shaderV0-V2, unused
                "data2 = vec4(0.0);\n" + // shadow-data aka unused for instanced lights
                // cutoff = 0 -> scale onto the whole screen, has effect everywhere
                "if(isDirectional && data1.z <= 0.0){\n" +
                "   gl_Position = vec4(positions.xy, 0.5, 1.0);\n" +
                "} else {\n" +
                "   mat4x3 localTransform = loadMat4x3(instanceTrans0,instanceTrans1,instanceTrans2);\n" +
                "   vec3 localPosition = positions;\n" +
                "   if(isSpotLight){\n" +
                "       float coneAngle = lightData1.x;\n" +
                "       localPosition.xy *= coneAngle;\n" +
                "   }\n" +
                "   vec3 finalPosition = matMul(localTransform, vec4(localPosition, 1.0));\n" +
                "   gl_Position = matMul(transform, vec4(finalPosition, 1.0));\n" +
                "}\n" +
                "invInsTrans0v = invInsTrans0;\n" +
                "invInsTrans1v = invInsTrans1;\n" +
                "invInsTrans2v = invInsTrans2;\n" +
                "uvw = gl_Position.xyw;\n"
    ).add(loadMat4x3)

    val vertexNI = ShaderStage(
        "v", listOf(
            Variable(GLSLType.V3F, "positions", VariableMode.ATTR),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.M4x3, "localTransform"),
            Variable(GLSLType.V1F, "cutoff"),
            Variable(GLSLType.V4F, "data1").flat(),
            Variable(GLSLType.V1B, "isSpotLight"),
            Variable(GLSLType.V3F, "uvw", VariableMode.OUT)
        ), "" +
                // cutoff = 0 -> scale onto the whole screen, has effect everywhere
                "if(cutoff <= 0.0){\n" +
                "   gl_Position = vec4(positions.xy, 0.5, 1.0);\n" +
                "} else {\n" +
                "   vec3 localPosition = positions;\n" +
                "   if(isSpotLight){\n" +
                "       float coneAngle = data1.x;\n" +
                "       localPosition.xy *= coneAngle;\n" +
                "   }\n" +
                "   vec3 finalPosition = matMul(localTransform, vec4(localPosition, 1.0));\n" +
                "   gl_Position = matMul(transform, vec4(finalPosition, 1.0));\n" +
                "}\n" +
                "uvw = gl_Position.xyw;\n"
    )

    private val shaderCache = HashMap<Pair<DeferredSettings, Int>, Shader>()

    fun createMainFragmentStage(type: LightType, isInstanced: Boolean): ShaderStage {
        val withShadows = !isInstanced
        val cutoffKeyword = "discard"
        val coreFragment = LightType.getShaderCode(type, cutoffKeyword, withShadows)
        val fragment = ShaderStage(
            "ls-f", listOf(
                Variable(GLSLType.V4F, "data0").flat(),
                Variable(GLSLType.V4F, "data1").flat(),
                Variable(GLSLType.V4F, "data2").flat(), // only if with shadows
                // light maps for shadows
                // - spotlights, directional lights
                Variable(GLSLType.S2DAShadow, "shadowMapPlanar", 1),
                // - point lights
                Variable(GLSLType.SCubeShadow, "shadowMapCubic", 1),
                Variable(GLSLType.V1B, "receiveShadows"),
                Variable(GLSLType.V1B, "canHaveShadows"),
                Variable(GLSLType.V3F, "finalPosition"),
                Variable(GLSLType.V3F, "finalNormal"),
                Variable(GLSLType.V1F, "finalReflectivity"),
                Variable(GLSLType.V1F, "finalSheen"),
                Variable(GLSLType.V1F, "finalTranslucency"),
                Variable(GLSLType.M4x3, "camSpaceToLightSpace"), // invLightMatrices[i]
                Variable(GLSLType.V3F, "cameraPosition"),
                Variable(GLSLType.V3F, "light", VariableMode.OUT)
            ) + depthVars, "" +
                    // light calculation including shadows if !instanced
                    startLightSum +
                    "vec3 V = -normalize(rawCameraDirection(uv));\n" +
                    "float NdotV = dot(finalNormal,V);\n" +
                    "int shadowMapIdx0 = 0;\n" + // always 0 at the start
                    "int shadowMapIdx1 = canHaveShadows ? int(data2.y) : 0;\n" +
                    // light properties, which are typically inside the loop
                    "vec3 lightColor = data0.rgb;\n" +
                    "vec3 lightPos = matMul(camSpaceToLightSpace, vec4(finalPosition, 1.0));\n" +
                    "vec3 lightNor = normalize(matMul(camSpaceToLightSpace, vec4(finalNormal, 0.0)));\n" +
                    "vec3 viewDir = normalize(matMul(camSpaceToLightSpace, vec4(finalPosition, 0.0)));\n" +
                    "float NdotL = 0.0;\n" + // normal dot light
                    "vec3 effectiveDiffuse = vec3(0.0), effectiveSpecular = vec3(0.0), lightDir = vec3(0.0,0.0,-1.0);\n" +
                    "float shaderV0 = data1.x, shaderV1 = data1.y, shaderV2 = data1.z, shaderV3 = data1.w;\n" +
                    coreFragment +
                    addSpecularLight +
                    addDiffuseLight +
                    mixAndClampLight
        ).add(quatRot).add(getReflectivity)
        return fragment
    }

    val visualizeLightCountShader = Shader(
        "visualize-light-count",
        listOf(
            Variable(GLSLType.V3F, "positions", VariableMode.ATTR),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.M4x3, "localTransform"),
            Variable(GLSLType.V1B, "isSpotLight"),
            Variable(GLSLType.V4F, "data1").flat(),
            Variable(GLSLType.V1B, "fullscreen"),
        ), "" +
                "void main(){\n" +
                // cutoff = 0 -> scale onto the whole screen, has effect everywhere
                "   if(fullscreen){\n" +
                "      gl_Position = vec4(positions.xy, 0.5, 1.0);\n" +
                "   } else {\n" +
                "       vec3 localPosition = positions;\n" +
                "       if(isSpotLight){\n" +
                "           float coneAngle = data1.x;\n" +
                "           localPosition.xy *= coneAngle;\n" +
                "       }\n" +
                "       vec3 finalPosition = matMul(localTransform, vec4(localPosition, 1.0));\n" +
                "      gl_Position = matMul(transform, vec4(finalPosition, 1.0));\n" +
                "   }\n" +
                "}\n", emptyList(), listOf(
            Variable(GLSLType.V1F, "countPerPixel"),
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ), "void main(){ result = vec4(countPerPixel); }"
    )

    val visualizeLightCountShaderInstanced = Shader(
        "visualize-light-count-instanced", listOf(
            Variable(GLSLType.V3F, "positions", VariableMode.ATTR),
            Variable(GLSLType.V4F, "instanceTrans0", VariableMode.ATTR),
            Variable(GLSLType.V4F, "instanceTrans1", VariableMode.ATTR),
            Variable(GLSLType.V4F, "instanceTrans2", VariableMode.ATTR),
            Variable(GLSLType.V4F, "lightData1", VariableMode.ATTR),
            Variable(GLSLType.V4F, "lightData2", VariableMode.ATTR),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.V1B, "isDirectional"),
            Variable(GLSLType.V1B, "isSpotLight"),
        ), "" +
                loadMat4x3 +
                "void main(){\n" +
                // cutoff = 0 -> scale onto the whole screen, has effect everywhere
                "   if(isDirectional && lightData1.z <= 0.0){\n" +
                "      gl_Position = vec4(positions.xy, 0.5, 1.0);\n" +
                "   } else {\n" +
                "       mat4x3 localTransform = loadMat4x3(instanceTrans0,instanceTrans1,instanceTrans2);\n" +
                "       vec3 localPosition = positions;\n" +
                "       if(isSpotLight){\n" +
                "           float coneAngle = lightData1.x;\n" +
                "           localPosition.xy *= coneAngle;\n" +
                "       }\n" +
                "       vec3 finalPosition = matMul(localTransform, vec4(localPosition, 1.0));\n" +
                "       gl_Position = matMul(transform, vec4(finalPosition, 1.0));\n" +
                "   }\n" +
                "}\n", emptyList(), listOf(
            Variable(GLSLType.V1F, "countPerPixel"),
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ), "void main(){ result = vec4(countPerPixel); }"
    )

    val uvwStage = ShaderStage(
        "uv2uvw", listOf(
            Variable(GLSLType.V3F, "uvw", VariableMode.IN),
            Variable(GLSLType.V2F, "uv", VariableMode.OUT),
            Variable(GLSLType.V2F, "invScreenSize")
        ), "uv = gl_FragCoord.xy * invScreenSize;\n"
    )

    val invStage = ShaderStage(
        "invTrans2cs2ls", listOf(
            Variable(GLSLType.V4F, "invInsTrans0v", VariableMode.IN).flat(),
            Variable(GLSLType.V4F, "invInsTrans1v", VariableMode.IN).flat(),
            Variable(GLSLType.V4F, "invInsTrans2v", VariableMode.IN).flat(),
            Variable(GLSLType.M4x3, "camSpaceToLightSpace", VariableMode.OUT)
        ), "camSpaceToLightSpace = loadMat4x3(invInsTrans0v,invInsTrans1v,invInsTrans2v);\n"
    ).add(loadMat4x3)

    fun getShader(settingsV2: DeferredSettings, type: LightType): Shader {
        val isInstanced = GFXState.instanceData.currentValue != MeshInstanceData.DEFAULT
        val useMSAA = useMSAA
        val key = type.ordinal * 4 + useMSAA.toInt(2) + isInstanced.toInt()
        return shaderCache.getOrPut(settingsV2 to key) {

            val builder = ShaderBuilder("Light-$type-$isInstanced")
            builder.addVertex(if (isInstanced) vertexI else vertexNI)
            if (isInstanced) builder.addFragment(invStage)
            builder.addFragment(uvwStage)
            builder.addFragment(
                ShaderStage(
                    "uv2depth",
                    listOf(
                        Variable(GLSLType.V2F, "uv"),
                        Variable(if (useMSAA) GLSLType.S2DMS else GLSLType.S2D, "depthTex"),
                        Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT)
                    ), if (useMSAA) {
                        "finalPosition = rawDepthToPosition(uv,texelFetch(depthTex,ivec2(uv*vec2(textureSize(depthTex))),gl_SampleID).x);\n"
                    } else {
                        "finalPosition = rawDepthToPosition(uv,texture(depthTex,uv).x);\n"
                    }
                )
            )
            val fragment = createMainFragmentStage(type, isInstanced)
            // deferred inputs: find deferred layers, which exist, and appear in the shader
            val deferredCode = StringBuilder()
            val deferredVariables = ArrayList<Variable>()
            deferredVariables += Variable(GLSLType.V2F, "uv")
            val imported = HashSet<String>()
            val sampleVariableName = if (useMSAA) "gl_SampleID" else null
            val samplerType = if (useMSAA) GLSLType.S2DMS else GLSLType.S2D
            for (layer in settingsV2.semanticLayers) {
                // if this layer is present,
                // then define the output,
                // and write the mapping
                val glslName = layer.type.glslName
                if (fragment.variables.any2 { it.name == glslName }) {
                    val lType = layer.type
                    deferredVariables.add(Variable(floats[lType.workDims - 1], lType.glslName, VariableMode.OUT))
                    layer.appendMapping(deferredCode, "", "Tmp", "", "uv", imported, sampleVariableName)
                }
            }
            deferredVariables += imported.map { Variable(samplerType, it, VariableMode.IN) }
            deferredVariables += depthVars
            val deferredStage = ShaderStage("deferred", deferredVariables, deferredCode.toString())
                .add(rawToDepth).add(depthToPosition).add(quatRot).add(octNormalPacking)
            builder.addFragment(deferredStage)
            builder.addFragment(fragment)
            if (useMSAA) builder.glslVersion = 400 // required for gl_SampleID
            builder.create(getKey(), "lht${type.ordinal}")
        }
    }
}