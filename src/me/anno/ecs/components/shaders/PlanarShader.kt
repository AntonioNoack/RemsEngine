package me.anno.ecs.components.shaders

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.maths.Maths.hasFlag

object PlanarShader : ECSMeshShader("planar") {

    override fun createFragmentVariables(flags: Int): ArrayList<Variable> {
        val list = super.createFragmentVariables(flags)
        list.add(Variable(GLSLType.V3F, "tilingU"))
        list.add(Variable(GLSLType.V3F, "tilingV"))
        list.add(Variable(GLSLType.V3F, "tileOffset"))
        return list
    }

    override fun createFragmentStages(flags: Int): List<ShaderStage> {
        return listOf(
            ShaderStage(
                "material",
                createFragmentVariables(flags)
                    .filter { it.name != "uv" },
                discardByCullingPlane +

                        // todo local position option
                        "vec3 colorPos = finalPosition - tileOffset;\n" +
                        "vec2 uv = vec2(dot(colorPos, tilingU), sign(finalNormal.y) * dot(colorPos, tilingV));\n" +

                        baseColorCalculation +
                        "finalNormal    = normalize(gl_FrontFacing ? normal : -normal);\n" +
                        // order and signs correct???
                        "finalTangent   = normalize(cross(finalNormal,tilingU));\n" +
                        "finalBitangent = normalize(cross(finalNormal,tilingV));\n" +
                        normalMapCalculation +
                        emissiveCalculation +
                        occlusionCalculation +
                        metallicCalculation +
                        roughnessCalculation +
                        v0 + sheenCalculation +
                        clearCoatCalculation +
                        reflectionCalculation +
                        finalMotionCalculation
            )
        )
    }

}