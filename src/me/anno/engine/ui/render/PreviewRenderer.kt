package me.anno.engine.ui.render

import me.anno.ecs.components.light.sky.shaders.FixedSkyShader.fixedSkyCode
import me.anno.ecs.components.light.sky.shaders.SkyShader.Companion.funcFBM
import me.anno.ecs.components.light.sky.shaders.SkyShader.Companion.funcHash
import me.anno.ecs.components.light.sky.shaders.SkyShader.Companion.funcNoise
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToLinear
import me.anno.engine.ui.render.ECSMeshShader.Companion.colorToSRGB
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.engine.ui.render.Renderers.finalResultStage
import me.anno.engine.ui.render.Renderers.tonemapGLSL
import me.anno.gpu.deferred.PBRLibraryGLTF.specularBRDFv2NoDivInlined2End
import me.anno.gpu.deferred.PBRLibraryGLTF.specularBRDFv2NoDivInlined2Start
import me.anno.gpu.shader.BaseShader.Companion.DRAWING_SKY
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib.randomGLSL
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.utils.types.Booleans.hasFlag

object PreviewRenderer : Renderer("preview") {

    var exposure = 1f

    override fun bind(shader: Shader) {
        super.bind(shader)
        shader.v1f("exposure", exposure)
    }

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
                    Variable(GLSLType.V1F, "exposure"),
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
                        "finalColor = tonemapLinear(exposure * finalColor);\n" +
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