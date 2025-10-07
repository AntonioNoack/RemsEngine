package me.anno.ecs.components.text

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RendererLib
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable

/**
 * averaging filter to get better results?
 * kills small details and corners, but the edges look better :)
 * */
object SDFAvgShader : ECSMeshShader("SDF-AVG") {

    // todo the text is too small with small text sizes:
    //  before reading a value, we first need to blur the texture...

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return listOf(
            ShaderStage(
                "material",
                createFragmentVariables(key) +
                       Variable(GLSLType.V4F, "cameraRotation"),
                concatDefines(key).toString() +
                        discardByCullingPlane +
                        // step by step define all material properties
                        "vec2 duv = 0.5/textureSize(diffuseMap,0), duv1 = vec2(duv.x,-duv.y);\n" +
                        "vec4 c0 = texture(diffuseMap,uv+duv);\n" +
                        "vec4 c1 = texture(diffuseMap,uv+duv1);\n" +
                        "vec4 c2 = texture(diffuseMap,uv-duv);\n" +
                        "vec4 c3 = texture(diffuseMap,uv-duv1);\n" +
                        "float sdf = c0.a + c1.a + c2.a + c3.a;\n" +
                        // if any color is emoji, all colors should be evaluated like that
                        "if (sdf > 0.0) {\n" + // emoji
                        "   finalAlpha = step(2.0, sdf);\n" +
                        "   if(finalAlpha < 0.5) discard;\n" +

                        "   c0 = getEmojiColor(c0);\n" +
                        "   c1 = getEmojiColor(c1);\n" +
                        "   c2 = getEmojiColor(c2);\n" +
                        "   c3 = getEmojiColor(c3);\n" +

                        "   vec4 baseColor = c0;\n" +
                        "   if (c1.a > baseColor.a) baseColor = c1;\n" +
                        "   if (c2.a > baseColor.a) baseColor = c2;\n" +
                        "   if (c3.a > baseColor.a) baseColor = c3;\n" +
                        "   finalColor = min(baseColor.rgb / baseColor.a, vec3(1.0));\n" +
                        "} else {\n" + // text
                        "   sdf = getTextColor(c0) + getTextColor(c1) + getTextColor(c2) + getTextColor(c3);\n" +
                        "   finalAlpha = step(2.0, sdf);\n" +
                        "   if(finalAlpha < 0.5) discard;\n" +
                        "   finalColor = diffuseBase.rgb;\n" +
                        "}" +
                        normalTanBitanCalculation +
                        normalMapCalculation +
                        emissiveCalculation +
                        occlusionCalculation +
                        metallicCalculation +
                        roughnessCalculation +
                        v0 + sheenCalculation +
                        clearCoatCalculation +
                        reflectionCalculation +
                        finalMotionCalculation
            ).add(ShaderLib.quatRot).add(ShaderLib.brightness).add(ShaderLib.parallaxMapping)
                .add(RendererLib.getReflectivity).add("" +
                        "float getTextColor(vec4 read) {\n" +
                        "   if (read.a > 0.0) {\n" + // emoji
                        "       return 0.0;\n" +
                        "   } else {\n" + // text
                        "       return read.g;\n" +
                        "   }\n" +
                        "}\n"+
                        "vec4 getEmojiColor(vec4 read) {\n" +
                        "    return read;\n" +
                        "}\n")
        )
    }
}