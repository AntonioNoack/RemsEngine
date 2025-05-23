package me.anno.engine.ui.render

import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

object ECSShaderLib {

    val simpleShader = BaseShader(
        "SimpleECS", listOf(
            Variable(GLSLType.V3F, "positions", VariableMode.ATTR),
            Variable(GLSLType.M4x4, "transform")
        ), "gl_Position = matMul(transform, vec4(positions, 1.0));", emptyList(), listOf(
            Variable(GLSLType.V4F, "diffuseBase"),
            Variable(GLSLType.V3F, "finalEmissive", VariableMode.OUT),
            Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
            Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
        ), "" +
                "finalEmissive = diffuseBase.rgb * 10.0;\n" +
                "finalColor = diffuseBase.rgb;\n" +
                "finalAlpha = diffuseBase.a;\n"
    )

    val pbrModelShader = ECSMeshShader("ECSMeshShader")
    val pbrModelShaderLight = ECSMeshShaderLight("ECSMeshShaderLight")
}