package me.anno.mesh.fbx.model

import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib
import me.anno.gpu.shader.ShaderPlus
import me.anno.utils.Maths

object FBXShader {
    const val maxWeightsDefault = 4 // 1 .. 4
    fun getShader(
        v3DBase: String, positionPostProcessing: String,
        y3D: String, getTextureLib: String
    ): ShaderPlus {
        val maxBones = Maths.clamp((GFX.maxVertexUniforms - (16 * 3)) / 16, 4, 256)
        return ShaderLib.createShaderPlus(
            "fbx", v3DBase +
                    "a3 coords;\n" +
                    "a2 uvs;\n" +
                    "a3 normals;\n" +
                    "a1 materialIndex;\n" +
                    "in ivec4 weightIndices;\n" +
                    "a4 weightValues;\n" +
                    "uniform mat4x4 transforms[$maxBones];\n" +
                    "void main(){\n" +
                    "   vec3 localPosition0 = (transforms[weightIndices.x] * vec4(coords, 1.0)).xyz * weightValues.x;\n" + //  * weightValues.x
                    "   if(weightValues.y > 0.01) localPosition0 += (transforms[weightIndices.y] * vec4(coords, 1.0)).xyz * weightValues.y;\n" +
                    "   if(weightValues.z > 0.01) localPosition0 += (transforms[weightIndices.z] * vec4(coords, 1.0)).xyz * weightValues.z;\n" +
                    "   if(weightValues.w > 0.01) localPosition0 += (transforms[int(weightIndices.w)] * vec4(coords, 1.0)).xyz * weightValues.w;\n" +
                    "   localPosition = localPosition0;\n" +
                    "   gl_Position = transform * vec4(localPosition0, 1.0);\n" + // already include second transform? yes, we should probably do that
                    "   uv = uvs;\n" +
                    "   normal = normals;\n" +
                    positionPostProcessing +
                    "}", y3D + "" +
                    "varying vec3 normal;\n", "" +
                    "uniform vec4 tint;" +
                    "uniform sampler2D tex;\n" +
                    getTextureLib +
                    ShaderLib.getColorForceFieldLib +
                    "void main(){\n" +
                    "   vec4 color = getTexture(tex, uv);\n" +
                    "   color.rgb *= 0.5 + 0.5 * dot(vec3(1.0, 0.0, 0.0), normal);\n" +
                    "   if(${ShaderLib.hasForceFieldColor}) color *= getForceFieldColor();\n" +
                    "   gl_FragColor = tint * color;\n" +
                    "}", listOf()
        )
    }
}