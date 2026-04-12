package me.anno.ecs.components.mesh.material.shaders

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

/**
 * Implements Silhouette Parallax Occlusion Mapping for typical PBR materials
 * */
object ParallaxShader : ECSMeshShader("parallax") {
    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return key.vertexData.onFragmentShader + key.instanceData.onFragmentShader + listOf(
            ShaderStage(
                "parallax",
                createFragmentVariables(key) + listOf(
                    Variable(GLSLType.S2D, "heightMap"),
                    Variable(GLSLType.V1F, "parallaxScale"),
                    Variable(GLSLType.V1F, "parallaxBias"),
                    Variable(GLSLType.V1I, "minParallaxSteps"),
                    Variable(GLSLType.V1I, "maxParallaxSteps"),
                    Variable(GLSLType.V2F, "uv", VariableMode.INOUT)
                ),
                concatDefines(key).toString() +
                        discardByCullingPlane +
                        normalTanBitanCalculation +
                        "if (parallaxScale > 0.0) {\n" +
                        "   vec3 tsV = normalize(vec3(dot(finalPosition, finalTangent), dot(finalPosition, finalBitangent), dot(finalPosition, finalNormal)));\n" +
                        "   tsV.xy /= -tsV.z;\n" + 
                        "   float numSteps = mix(float(maxParallaxSteps), float(minParallaxSteps), abs(tsV.z));\n" +
                        "   float stepHeight = 1.0 / numSteps;\n" +
                        "   vec2 deltaUV = tsV.xy * parallaxScale * stepHeight;\n" +
                        "   vec2 currUV = uv + tsV.xy * parallaxScale * parallaxBias;\n" +
                        "   float currLayerHeight = 1.0;\n" +
                        "   float currHeight = texture(heightMap, currUV).r;\n" +
                        "   while(currLayerHeight > currHeight && currLayerHeight > 0.0) {\n" +
                        "       currLayerHeight -= stepHeight;\n" +
                        "       currUV += deltaUV;\n" +
                        "       currHeight = texture(heightMap, currUV).r;\n" +
                        "   }\n" +
                        "   vec2 prevUV = currUV - deltaUV;\n" +
                        "   float nextH = currHeight - currLayerHeight;\n" +
                        "   float prevH = texture(heightMap, prevUV).r - (currLayerHeight + stepHeight);\n" +
                        "   float weight = nextH / (nextH - prevH);\n" +
                        "   uv = mix(currUV, prevUV, weight);\n" +
                        "   if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) discard;\n" +
                        "}\n" +
                        baseColorCalculation +
                        createColorFragmentStage()
            ).add(getReflectivity)
        )
    }
}