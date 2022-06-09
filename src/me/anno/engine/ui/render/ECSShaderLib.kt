package me.anno.engine.ui.render

import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

object ECSShaderLib {

    lateinit var pbrModelShader: BaseShader
    lateinit var clearingPbrModelShader: BaseShader

    fun init() {

        // todo automatic texture mapping and indices
        // todo use shader stages for that everywhere

        val shader = ECSMeshShader("model")
        pbrModelShader = shader
        shader.ignoreUniformWarnings(
            listOf(
                "finalSheen", "finalTranslucency", "metallicMinMax",
                "emissiveBase", "normalStrength", "ambientLight",
                "occlusionStrength", "invLocalTransform",
                "numberOfLights", "roughnessMinMax", "finalClearCoat",
                "worldScale"
            )
        )

        shader.glslVersion = 330

        val clearingShader = BaseShader(
            "clear-pbr", listOf(
                Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
                Variable(GLSLType.M4x4, "transform"),
                Variable(GLSLType.M4x4, "prevTransform"),
            ), "" +
                    "void main(){\n" +
                    "   vec3 finalPosition = coords;\n" +
                    "   finalNormal = normalize(coords);\n" +
                    "   gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                    // motion vectors
                    "   currPosition = gl_Position.xyw;\n" +
                    "   prevPosition = (prevTransform * vec4(finalPosition, 1.0)).xyw;\n" +
                    "   finalPosition *= 1e36;\n" +
                    "}",
            listOf(
                Variable(GLSLType.V3F, "finalNormal"),
                Variable(GLSLType.V3F, "currPosition"),
                Variable(GLSLType.V3F, "prevPosition")
            ), listOf(
                Variable(GLSLType.V3F, "finalColor", VariableMode.INOUT),
                Variable(GLSLType.V1F, "finalAlpha", VariableMode.INOUT),
            ), "" +
                    "void main(){\n" +
                    // tricking the detection for variable definitions,
                    // because it doesn't check the varyings, it seems
                    "   // finalNormal, finalMotion, finalColor, finalAlpha\n" +
                    "   vec2 finalMotion = currPosition.xy/currPosition.z - prevPosition.xy/prevPosition.z;\n" +
                    "   vec3 finalPosition = finalNormal * 1e36;\n" + // 1e38 is max for float
                    "}"
        )
        clearingShader.glslVersion = 330
        clearingShader.ignoreUniformWarnings("normals", "tint", "uvs", "colors", "drawMode", "tangents")
        clearingPbrModelShader = clearingShader

    }

}