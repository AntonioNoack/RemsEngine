package me.anno.ecs.components.text

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable

object SDFShader : ECSMeshShader("SDF") {
    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return listOf(
            ShaderStage(
                "material",
                createFragmentVariables(key) +
                        listOf(
                            Variable(GLSLType.V4F, "cameraRotation"),
                            Variable(GLSLType.V1B, "invertSDF")
                        ),
                concatDefines(key).toString() +
                        discardByCullingPlane +
                        // step by step define all material properties
                        // to do smoothstep (? would need transparency, and that's an issue...)
                        // to do smoothstep for non-deferred mode?
                        "finalAlpha = step(texture(diffuseMap,uv).x,0.5);\n" +
                        "if(invertSDF) finalAlpha = 1.0 - finalAlpha;\n" +
                        "if(finalAlpha < 0.5) discard;\n" +
                        "finalColor = vertexColor0.rgb * diffuseBase.rgb;\n" +
                        normalTanBitanCalculation +
                        normalMapCalculation +
                        emissiveCalculation +
                        occlusionCalculation +
                        metallicCalculation +
                        roughnessCalculation +
                        v0 + sheenCalculation +
                        clearCoatCalculation +
                        reflectionCalculation +
                        finalMotionCalculation
            ).add(ShaderLib.quatRot).add(ShaderLib.brightness).add(ShaderLib.parallaxMapping)
        )
    }
}