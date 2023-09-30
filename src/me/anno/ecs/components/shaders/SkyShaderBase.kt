package me.anno.ecs.components.shaders

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

open class SkyShaderBase(name: String) : ECSMeshShader(name) {
    companion object {
        val motionVectorCode2 = "" +
                motionVectorInit +
                "#ifdef MOTION_VECTORS\n" +
                "   currPosition = gl_Position;\n" +
                "   prevPosition = matMul(prevTransform, vec4(prevLocalPosition, 1.0));\n" +
                "#endif\n"
    }

    override fun createVertexStages(flags: Int): List<ShaderStage> {
        val defines = createDefines(flags).toString()
        return listOf(
            ShaderStage(
                "vertex",
                createVertexVariables(flags) +
                        listOf(
                            Variable(GLSLType.V1F, "meshScale"),
                            Variable(GLSLType.V1B, "reversedDepth"),
                            Variable(GLSLType.V1B, "isPerspective"),
                            Variable(GLSLType.V4F, "currPosition", VariableMode.OUT),
                            Variable(GLSLType.V4F, "prevPosition", VariableMode.OUT),
                        ),
                defines +
                        "localPosition = coords;\n" +
                        "finalPosition = meshScale * localPosition;\n" +
                        "#ifdef COLORS\n" +
                        "   normal = -coords;\n" +
                        "#endif\n" +
                        "gl_Position = matMul(transform, vec4(finalPosition, 1.0));\n" +
                        motionVectorCode2 +
                        "if(isPerspective) gl_Position.z = (reversedDepth ? 1e-36 : 0.9999995) * gl_Position.w;\n" +
                        ShaderLib.positionPostProcessing
            )
        )
    }

    override fun createFragmentStages(flags: Int): List<ShaderStage> {
        // todo the red clouds in the night sky are a bit awkward
        val stage = ShaderStage(
            "skyBase", listOf(
                Variable(GLSLType.V3F, "finalNormal", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
                Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalEmissive", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalMotion", VariableMode.OUT),
                Variable(GLSLType.V3F, "normal"),
                Variable(GLSLType.V4F, "currPosition"),
                Variable(GLSLType.V4F, "prevPosition"),
                Variable(GLSLType.V4F, "worldRot"),
                Variable(GLSLType.V3F, "skyColor"),
                Variable(GLSLType.V4F, "currPosition"),
                Variable(GLSLType.V4F, "prevPosition"),
            ), createDefines(flags).toString() +
                    // sky no longer properly defined for y > 0
                    "finalNormal = normalize(-normal);\n" +
                    // sky color can be quite expensive to compute, so only do so if we need it
                    "#ifdef COLORS\n" +
                    "   finalColor = vec3(0.0);\n" +
                    "   finalEmissive = getSkyColor(quatRot(finalNormal, worldRot));\n" +
                    "#endif\n" +
                    "finalNormal = -finalNormal;\n" +
                    "finalPosition = finalNormal * 1e20;\n" +
                    finalMotionCalculation
        )
        stage.add(quatRot)
        stage.add(getSkyColor())
        return listOf(stage)
    }

    open fun getSkyColor(): String = "vec3 getSkyColor(vec3 pos){ return skyColor; }\n"
}