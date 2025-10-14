package me.anno.engine.ui.render

import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

object ECSShaderLib {

    val simpleShader = BaseShader(
        "SimpleECS", listOf(
            Variable(GLSLType.V3F, "positions", VariableMode.ATTR),
            Variable(GLSLType.V4F, "colors0", VariableMode.ATTR),
            Variable(GLSLType.M4x4, "transform"),
            Variable(GLSLType.V1I, "hasVertexColors")
        ), "" +
                "gl_Position = matMul(transform, vec4(positions, 1.0));\n" +
                "vertexColor0 = (hasVertexColors & 1) != 0 ? colors0 : vec4(1.0);\n", listOf(
            Variable(GLSLType.V4F, "vertexColor0"),
        ), listOf(
            Variable(GLSLType.V4F, "diffuseBase"),
            Variable(GLSLType.V3F, "finalEmissive", VariableMode.OUT),
            Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
        ), "" +
                "vec4 color = diffuseBase * vertexColor0;\n" +
                "finalEmissive = color.rgb * 10.0;\n" +
                "finalColor = color.rgb;\n" +
                "finalAlpha = color.a;\n"
    )

    val pbrModelShader = ECSMeshShader("ECSMeshShader")
    val pbrModelShaderLight = ECSMeshShaderLight("ECSMeshShaderLight")
}