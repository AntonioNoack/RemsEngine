package me.anno.ecs.components.shaders

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable

object TriplanarShader : ECSMeshShader("triplanar") {

    override fun createFragmentVariables(
        isInstanced: Boolean,
        isAnimated: Boolean,
        motionVectors: Boolean
    ): ArrayList<Variable> {
        val list = super.createFragmentVariables(isInstanced, isAnimated, motionVectors)
        list.add(Variable(GLSLType.V4F, "primaryTiling"))
        list.add(Variable(GLSLType.V1F, "sharpness"))
        list.add(Variable(GLSLType.V1F, "blendPreferY"))
        list.add(Variable(GLSLType.V3F, "cameraPosition"))
        list.add(Variable(GLSLType.V1F, "worldScale"))
        return list
    }

    override fun createFragmentStages(
        isInstanced: Boolean,
        isAnimated: Boolean,
        motionVectors: Boolean
    ): List<ShaderStage> {
        return listOf(
            ShaderStage(
                "material",
                createFragmentVariables(isInstanced, isAnimated, motionVectors),
                discardByCullingPlane +
                        // step by step define all material properties
                        normalTanBitanCalculation +
                        "vec3 blend = vec3(0.0);\n" +
                        "if(blendPreferY > 0.0){\n" +
                        "   blend.xz = max(abs(normalize(finalNormal.xz)) - 0.7071 * sharpness, 0.0);\n" +
                        // todo it would be nice, if sharpness had the exactly same effect on both axes
                        "   blend.y = clamp((abs(finalNormal.y) - blendPreferY) * (1.0 + 200.0 * pow(sharpness, 16.0)), 0.0, 1.0);\n" +
                        "   blend.xz *= (1.0 - blend.y) / max(0.00001, blend.x + blend.z);\n" +
                        "} else {\n" +
                        "   blend = max(abs(finalNormal) - max(max(finalNormal.x,finalNormal.y),finalNormal.z) * sharpness, 0.0);\n" +
                        "   blend /= blend.x + blend.y + blend.z;\n" +
                        "}\n" +

                        // todo local position option

                        "#define LOAD_TRIPLANAR_TEXTURE(tex,uvx,uvy,uvz) (texture(tex,uvx)*blend.x+texture(tex,uvy)*blend.y+texture(tex,uvz)*blend.z)\n" +
                        "vec3 worldPos = finalPosition / worldScale + cameraPosition;\n" +
                        "vec2 uvx = vec2(-sign(finalNormal.x) * worldPos.z, -worldPos.y) * primaryTiling.xy + primaryTiling.zw;\n" +
                        "vec2 uvy = vec2(+worldPos.x, +sign(finalNormal.y) * worldPos.z) * primaryTiling.xy + primaryTiling.zw;\n" +
                        "vec2 uvz = vec2(+sign(finalNormal.z) * worldPos.x, -worldPos.y) * primaryTiling.xy + primaryTiling.zw;\n" +

                        // baseColorCalculation +
                        "vec4 texDiffuseMap = LOAD_TRIPLANAR_TEXTURE(diffuseMap,uvx,uvy,uvz);\n" +
                        "vec4 color = vec4(vertexColor0.rgb, 1.0) * diffuseBase * texDiffuseMap;\n" +
                        "if(color.a < ${1f / 255f}) discard;\n" +
                        // "finalColor = blend;\n" + // for debugging
                        // "finalColor = vec3(fract(uvx*blend.x+uvy*blend.y+uvz*blend.z), 0.0);\n" + // for debugging
                        "finalColor = color.rgb;\n" +
                        "finalAlpha = color.a;\n" +

                        // todo detail maps

                        // normalMapCalculation +
                        "#define scaleNormal(v2, scale) normalize(vec3((v2 - 0.5) * (scale * normalStrength.x), 1.0))\n" +
                        "mat3 tbn = mat3(finalTangent, finalBitangent, finalNormal);\n" +
                        "if(abs(normalStrength.x) > 0.0){\n" +
                        "   vec3 norX = scaleNormal(texture(normalMap,uvx).xy, blend.x);\n" +
                        "   vec3 norY = scaleNormal(texture(normalMap,uvy).xy, blend.y);\n" +
                        "   vec3 norZ = scaleNormal(texture(normalMap,uvz).xy, blend.z);\n" +
                        // normalize?
                        "   finalNormal = normalize(\n" +
                        "       finalNormal.x * vec3(norX.z, +sign(finalNormal.x)*norX.y, -norX.x) +\n" +
                        "       finalNormal.y * vec3(+sign(finalNormal.y)*norY.x, norY.z, -norY.y) +\n" + // todo top looks weaker, why?
                        "       finalNormal.z * vec3(+norZ.x, +sign(finalNormal.z)*norZ.y, norZ.z)\n" +
                        "   );\n" +
                        "}\n" +

                        // corrected properties
                        // emissiveCalculation +
                        "finalEmissive  = LOAD_TRIPLANAR_TEXTURE(emissiveMap,uvx,uvy,uvz).rgb * emissiveBase;\n" +
                        // occlusionCalculation +
                        "finalOcclusion = (1.0 - LOAD_TRIPLANAR_TEXTURE(occlusionMap,uvx,uvy,uvz).r) * occlusionStrength;\n" +
                        // metallicCalculation +
                        "finalMetallic  = clamp(mix(metallicMinMax.x,metallicMinMax.y,LOAD_TRIPLANAR_TEXTURE(metallicMap,uvx,uvy,uvz).r),0.0,1.0);\n" +
                        // roughnessCalculation +
                        "finalRoughness = clamp(mix(roughnessMinMax.x,roughnessMinMax.y,LOAD_TRIPLANAR_TEXTURE(roughnessMap,uvx,uvy,uvz).r),0.0,1.0);\n" +
                        reflectionPlaneCalculation +
                        v0 +
                        // sheenCalculation +
                        // sheen calculation
                        "if(sheen > 0.0){\n" +
                        "   vec3 sheenNormal = finalNormal;\n" +
                        // to do support sheen normal (?), how important is that?
                        /*"   if(finalSheen * normalStrength.y > 0.0){\n" +
                        "      vec3 normalFromTex = texture(sheenNormalMap, uv).rgb * 2.0 - 1.0;\n" +
                        "           normalFromTex = tbn * normalFromTex;\n" +
                        // original or transformed "finalNormal"? mmh...
                        // transformed probably is better
                        "      sheenNormal = mix(finalNormal, normalFromTex, normalStrength.y);\n" +
                        "   }\n" +*/
                        // calculate sheen
                        "   float sheenFresnel = 1.0 - abs(dot(sheenNormal,V0));\n" +
                        "   finalSheen = sheen * pow(sheenFresnel, 3.0);\n" +
                        "} else finalSheen = 0.0;\n" +
                        clearCoatCalculation +
                        (if (motionVectors) finalMotionCalculation else "")
            )
        )
    }

}