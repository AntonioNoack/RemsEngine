package me.anno.ecs.components.mesh

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

/**
 * defined attributes, and how they are transformed into data we render with
 * todo make this be the way we describe meshes
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
                        Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
                        Variable(GLSLType.V3F, "localPosition", VariableMode.OUT)
                    ), "localPosition = coords;\n"
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
                    "def-plp", listOf(
                        Variable(GLSLType.V3F, "localPosition"),
                        Variable(GLSLType.V3F, "prevLocalPosition", VariableMode.OUT)
                    ), "prevLocalPosition = localPosition;\n"
                )
            ),
            emptyList() // nothing to clean up
        )
    }
}