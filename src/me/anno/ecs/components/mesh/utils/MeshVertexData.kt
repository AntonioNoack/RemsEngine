package me.anno.ecs.components.mesh.utils

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

/**
 * defined attributes, and how they are transformed into data we render with
 * */
class MeshVertexData(
    val loadPosition: List<ShaderStage>,
    val loadNorTan: List<ShaderStage>,
    val loadColors: List<ShaderStage>,
    val loadMotionVec: List<ShaderStage>,
    val onFragmentShader: List<ShaderStage>
) {
    companion object {
        val DEFAULT = MeshVertexData(
            listOf(
                ShaderStage(
                    "def-lp", listOf(
                        Variable(GLSLType.V3F, "positions", VariableMode.ATTR),
                        Variable(GLSLType.V3F, "localPosition", VariableMode.OUT)
                    ), "localPosition = positions;\n"
                )
            ),
            listOf(
                ShaderStage(
                    "def-nor", listOf(
                        Variable(GLSLType.V3F, "normals", VariableMode.ATTR),
                        Variable(GLSLType.V3F, "normal", VariableMode.OUT),
                        Variable(GLSLType.V4F, "tangents", VariableMode.ATTR),
                        Variable(GLSLType.V4F, "tangent", VariableMode.OUT)
                    ),
                    "normal = normals;\n" +
                            "tangent = tangents;\n"
                )
            ),
            listOf(
                ShaderStage(
                    "def-col", listOf(
                        // todo attr/uniform depends
                        Variable(GLSLType.V4F, "colors0", VariableMode.ATTR),
                        Variable(GLSLType.V4F, "colors1"),
                        Variable(GLSLType.V4F, "colors2"),
                        Variable(GLSLType.V4F, "colors3"),
                        Variable(GLSLType.V4F, "vertexColor0", VariableMode.OUT),
                        Variable(GLSLType.V4F, "vertexColor1", VariableMode.OUT),
                        Variable(GLSLType.V4F, "vertexColor2", VariableMode.OUT),
                        Variable(GLSLType.V4F, "vertexColor3", VariableMode.OUT),
                        Variable(GLSLType.V1I, "hasVertexColors"),
                        Variable(GLSLType.V2F, "uvs", VariableMode.ATTR),
                        Variable(GLSLType.V2F, "uv", VariableMode.OUT),
                    ), "" +
                            "vertexColor0 = (hasVertexColors & 1) != 0 ? colors0 : vec4(1.0);\n" +
                            "vertexColor1 = (hasVertexColors & 2) != 0 ? colors1 : vec4(1.0);\n" +
                            "vertexColor2 = (hasVertexColors & 4) != 0 ? colors2 : vec4(1.0);\n" +
                            "vertexColor3 = (hasVertexColors & 8) != 0 ? colors3 : vec4(1.0);\n" +
                            "uv = uvs;\n"
                )
            ),
            listOf(
                ShaderStage(
                    "def-mov", listOf(
                        Variable(GLSLType.V3F, "localPosition"),
                        Variable(GLSLType.V3F, "prevLocalPosition", VariableMode.OUT)
                    ), "prevLocalPosition = localPosition;\n"
                )
            ),
            emptyList() // nothing to clean up
        )

        // calculate normals using cross product
        val flatNormalsNorTan = ShaderStage(
            "flat-nor", listOf(
                Variable(GLSLType.V3F, "normal", VariableMode.OUT),
                Variable(GLSLType.V4F, "tangent", VariableMode.OUT)
            ), "normal = vec3(0.0); tangent = vec4(0.0);\n"
        )

        val flatNormalsFragment = ShaderStage(
            "flat-px-nor", listOf(
                Variable(GLSLType.V3F, "finalPosition"),
                Variable(GLSLType.V2F, "uv"),
                Variable(GLSLType.V3F, "normal", VariableMode.OUT),
                Variable(GLSLType.V4F, "tangent", VariableMode.OUT)
            ), "" +
                    "vec3 dPdx = dFdx(finalPosition), dPdy = dFdy(finalPosition);\n" +
                    "normal = normalize(cross(dPdx, dPdy));\n" +
                    // tangent calculation by
                    // https://community.khronos.org/t/computing-the-tangent-space-in-the-fragment-shader/52861
                    "vec2 dUVdx = dFdx(uv), dUVdy = dFdy(uv);\n" +
                    "tangent = vec4(normalize(dPdx * dUVdy.t - dPdy * dUVdx.t), 1.0);\n"
        )

        val noColors = ShaderStage(
            "no-col", listOf(
                Variable(GLSLType.V4F, "vertexColor0", VariableMode.OUT),
                Variable(GLSLType.V4F, "vertexColor1", VariableMode.OUT),
                Variable(GLSLType.V4F, "vertexColor2", VariableMode.OUT),
                Variable(GLSLType.V4F, "vertexColor3", VariableMode.OUT),
                Variable(GLSLType.V2F, "uvs", VariableMode.ATTR),
                Variable(GLSLType.V2F, "uv", VariableMode.OUT),
            ), "" +
                    "vertexColor0 = vec4(1.0);\n" +
                    "vertexColor1 = vec4(1.0);\n" +
                    "vertexColor2 = vec4(1.0);\n" +
                    "vertexColor3 = vec4(1.0);\n" +
                    "uv = uvs;\n"
        )
    }
}