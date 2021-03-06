package me.anno.engine.ui.render

import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

object ECSShaderLib {

    lateinit var pbrModelShader: BaseShader
    lateinit var clearPbrModelShader: BaseShader

    fun init() {

        // todo automatic texture mapping and indices
        // todo use shader stages for that everywhere

        val shader = ECSMeshShader("model")
        pbrModelShader = shader
        shader.ignoreNameWarnings(
            "finalSheen", "finalTranslucency", "metallicMinMax",
            "emissiveBase", "normalStrength", "ambientLight",
            "occlusionStrength", "invLocalTransform",
            "numberOfLights", "roughnessMinMax", "finalClearCoat",
            "worldScale", "drawMode", "applyToneMapping",
            "colors"
        )

        clearPbrModelShader = object : ECSMeshShader("clear") {
            override fun createVertexStage(
                isInstanced: Boolean,
                isAnimated: Boolean,
                colors: Boolean,
                motionVectors: Boolean
            ): ShaderStage {
                return ShaderStage(
                    "vertex", listOf(
                        Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
                        Variable(GLSLType.M4x4, "transform"),
                        Variable(GLSLType.M4x4, "prevTransform"),
                        Variable(GLSLType.V4F, "currPosition", VariableMode.OUT),
                        Variable(GLSLType.V4F, "prevPosition", VariableMode.OUT),
                        Variable(GLSLType.V3F, "finalNormal", VariableMode.OUT),
                        Variable(GLSLType.V1F, "zDistance", VariableMode.OUT)
                    ), "" +
                            "gl_Position = transform * vec4(coords, 1.0);\n" +
                            "finalNormal = -coords;\n" +
                            "currPosition = gl_Position;\n" +
                            "prevPosition = (prevTransform * vec4(coords, 1.0));\n" +
                            ShaderLib.positionPostProcessing
                )
            }

            override fun createFragmentStage(
                isInstanced: Boolean,
                isAnimated: Boolean,
                motionVectors: Boolean
            ): ShaderStage {
                return ShaderStage(
                    "material",
                    listOf(
                        Variable(GLSLType.V3F, "finalNormal", VariableMode.INOUT),
                        Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT),
                        Variable(GLSLType.V4F, "currPosition"),
                        Variable(GLSLType.V4F, "prevPosition"),
                        Variable(GLSLType.V4F, "color"),
                        Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
                        Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
                        Variable(GLSLType.V3F, "finalMotion", VariableMode.OUT),
                    ),
                    "" +
                            "#define SKIP_LIGHTS\n" +
                            "finalNormal = normalize(finalNormal);\n" +
                            "finalPosition = -finalNormal * 1e36;\n" + // 1e38 is max for float
                            "finalMotion = currPosition.xyz/currPosition.w - prevPosition.xyz/prevPosition.w;\n" +
                            "finalColor = color.rgb;\n" +
                            "finalAlpha = color.a;\n"
                )
            }
        }.ignoreNameWarnings("drawMode", "tint", "normals", "uvs", "tangents", "colors")

    }

}