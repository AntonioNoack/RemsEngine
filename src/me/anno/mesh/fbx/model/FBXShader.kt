package me.anno.mesh.fbx.model

import me.anno.gpu.GFX
import me.anno.gpu.ShaderLib
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.shader.Shader
import me.anno.utils.Maths
import org.apache.logging.log4j.LogManager

object FBXShader {

    private val LOGGER = LogManager.getLogger(FBXShader::class)

    val attr = listOf(
        Attribute("coords", 3),
        Attribute("uvs", 2),
        Attribute("uvs", 2),
        Attribute("normals", 3),
        Attribute("materialIndex", AttributeType.UINT8, 1),
        Attribute("weightIndices", AttributeType.UINT8, 4),
        Attribute("weightValues", 4),
    )

    var maxBones = 0

    const val maxWeightsDefault = 4 // 1 .. 4
    fun getShader(
        v3DBase: String, positionPostProcessing: String,
        y3D: String, getTextureLib: String
    ): Shader {
        maxBones = Maths.clamp((GFX.maxVertexUniforms - (16 * 3)) / 16, 4, 256)
        LOGGER.info("Max number of bones: $maxBones")
        return ShaderLib.createShaderPlus(
            "fbx", v3DBase +
                    "a3 coords;\n" +
                    "a2 uvs;\n" +
                    "a3 normals;\n" +
                    "in int materialIndex;\n" +
                    "in ivec4 weightIndices;\n" +
                    "a4 weightValues;\n" +
                    "uniform mat4x3 transforms[$maxBones];\n" +
                    "uniform int boneLimit;\n" +
                    "void main(){\n" +
                    "   vec4 coords4 = vec4(coords, 1.0);\n" +
                    "   wei = weightValues;\n" +
                    "   float sum = 0;" +
                    "   mat4x3 skinTransform = transforms[min(weightIndices.x, boneLimit)] * weightValues.x;\n" +
                    "   skinTransform       += transforms[min(weightIndices.y, boneLimit)] * weightValues.y;\n" +
                    "   skinTransform       += transforms[min(weightIndices.z, boneLimit)] * weightValues.z;\n" +
                    "   skinTransform       += transforms[min(weightIndices.w, boneLimit)] * weightValues.w;\n" +
                    "   skinTransform /= dot(vec4(1), weightValues);\n" +
                    "   vec3 localPosition0 = (skinTransform * coords4).xyz;\n" +
                    "   localPosition = localPosition0;\n" +
                    "   gl_Position = transform * coords4;" +
                    "   gl_Position = transform * vec4(localPosition0, 1.0);\n" + // already include second transform? yes, we should probably do that
                    "   uv = uvs;\n" +
                    "   normal = skinTransform * vec4(normals, 0.0);\n" + // rotate normal as well; normalization may be instead required in the fragment shader
                    positionPostProcessing +
                    "}", y3D + "" +
                    "varying vec3 normal;\n" +
                    "varying vec4 wei;\n", "" +
                    "uniform vec4 tint;" +
                    "uniform sampler2D tex;\n" +
                    getTextureLib +
                    ShaderLib.getColorForceFieldLib +
                    "void main(){\n" +
                    "   vec4 color = vec4(normal*.5+.5,1);\ngetTexture(tex, uv);\n" +
                    "   color.a = 1;\n" +
                    "   color.rgb *= 0.5 + 0.5 * dot(vec3(1.0, 0.0, 0.0), normal);\n" +
                    "   if(${ShaderLib.hasForceFieldColor}) color *= getForceFieldColor();\n" +
                    "   gl_FragColor = tint * color;\n" +
                    "}", listOf()
        )
    }
}