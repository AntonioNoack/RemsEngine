package me.anno.ecs.components.shaders

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

open class SkyShaderBase(name: String) : ECSMeshShader(name) {

    companion object {
        val motionVectorCode2 = "" +
                "#ifdef MOTION_VECTORS\n" +
                "   currPosition = gl_Position;\n" +
                "   prevPosition = matMul(prevTransform, vec4(localPosition, 1.0));\n" +
                "#endif\n"
    }

    override fun createVertexStages(key: ShaderKey): List<ShaderStage> {
        val defines = concatDefines(key).toString()
        return listOf(
            ShaderStage(
                "vertex", listOf(
                    Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
                    Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT),
                    Variable(GLSLType.V3F, "localPosition", VariableMode.OUT),
                    Variable(GLSLType.V3F, "normal", VariableMode.OUT),
                    Variable(GLSLType.V1F, "meshScale"),
                    Variable(GLSLType.V1B, "reversedDepth"),
                    Variable(GLSLType.V1B, "isPerspective"),
                    Variable(GLSLType.V4F, "currPosition", VariableMode.OUT),
                    Variable(GLSLType.V4F, "prevPosition", VariableMode.OUT),
                    Variable(GLSLType.M4x4, "transform"),
                    Variable(GLSLType.M4x4, "prevTransform"),
                    Variable(GLSLType.V2F, "uvs", VariableMode.ATTR),
                    Variable(GLSLType.V2F, "uv", VariableMode.OUT)
                ),
                defines +
                        "localPosition = coords;\n" +
                        "finalPosition = meshScale * localPosition;\n" +
                        "#ifdef COLORS\n" +
                        "   normal = -coords;\n" +
                        "#endif\n" +
                        "gl_Position = matMul(transform, vec4(finalPosition, 1.0));\n" +
                        motionVectorCode2 +
                        "if(isPerspective) gl_Position.z = (reversedDepth ? 0.0 : 1.0) * gl_Position.w;\n" +
                        // uvs are used in CubemapSkybox
                        "#ifdef COLORS\n" +
                        "   uv = uvs;\n" +
                        "#endif\n"
            )
        )
    }

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        // todo the red clouds in the night sky are a bit awkward
        val stage = ShaderStage(
            "skyBase", listOf(
                Variable(GLSLType.V3F, "finalNormal", VariableMode.INOUT),
                Variable(GLSLType.V3F, "finalPosition", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalColor", VariableMode.OUT),
                Variable(GLSLType.V1F, "finalAlpha", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalEmissive", VariableMode.OUT),
                Variable(GLSLType.V3F, "finalMotion", VariableMode.OUT),
                Variable(GLSLType.V4F, "currPosition"),
                Variable(GLSLType.V4F, "prevPosition"),
                Variable(GLSLType.V4F, "worldRot"),
                Variable(GLSLType.V3F, "skyColor"),
                Variable(GLSLType.V4F, "currPosition"),
                Variable(GLSLType.V4F, "prevPosition"),
            ), concatDefines(key).toString() +
                    // sky no longer properly defined for y > 0
                    "finalNormal = normalize(finalNormal);\n" +
                    // sky color can be quite expensive to compute, so only do so if we need it
                    "#ifdef COLORS\n" +
                    "   finalColor = vec3(0.0);\n" +
                    "   finalEmissive = getSkyColor(-quatRot(finalNormal, worldRot));\n" +
                    "#endif\n" +
                    "finalPosition = finalNormal * 1e20;\n" +
                    finalMotionCalculation
        )
        stage.add(quatRot)
        stage.add(getSkyColor())
        return listOf(stage)
    }

    open fun getSkyColor(): String = "vec3 getSkyColor(vec3 pos){ return skyColor; }\n"
}