package me.anno.ecs.components.shaders

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable

object PlanarShader : ECSMeshShader("planar") {

    override fun createFragmentVariables(
        isInstanced: Boolean,
        isAnimated: Boolean,
        motionVectors: Boolean
    ): ArrayList<Variable> {
        val list = super.createFragmentVariables(isInstanced, isAnimated, motionVectors)
        list.add(Variable(GLSLType.V3F, "tilingU"))
        list.add(Variable(GLSLType.V3F, "tilingV"))
        list.add(Variable(GLSLType.V3F, "tileOffset"))
        return list
    }

    override fun createFragmentStages(
        isInstanced: Boolean,
        isAnimated: Boolean,
        motionVectors: Boolean
    ): List<ShaderStage> {
        super.createFragmentStages(isInstanced, isAnimated, motionVectors)
        return listOf(
            ShaderStage(
                "material",
                createFragmentVariables(isInstanced, isAnimated, motionVectors)
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
                        reflectionPlaneCalculation +
                        v0 + sheenCalculation +
                        clearCoatCalculation +
                        (if (motionVectors) finalMotionCalculation else "")
            )
        )
    }

}