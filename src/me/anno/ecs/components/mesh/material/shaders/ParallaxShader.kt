package me.anno.ecs.components.mesh.material.shaders

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.ShaderLib.applyTiling
import me.anno.gpu.shader.ShaderLib.parallaxMapping
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

/**
 * Implements Silhouette Parallax Occlusion Mapping for typical PBR materials
 * */
object ParallaxShader : ECSMeshShader("parallax") {
    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return key.vertexData.onFragmentShader + key.instanceData.onFragmentShader + listOf(
            ShaderStage(
                "parallax",
                createFragmentVariables(key) + listOf(
                    Variable(GLSLType.S2D, "parallaxMap"),
                    Variable(GLSLType.V1F, "parallaxScale"),
                    Variable(GLSLType.V1F, "parallaxBias"),
                    Variable(GLSLType.V4F, "parallaxTiling"),
                    Variable(GLSLType.V1I, "minParallaxSteps"),
                    Variable(GLSLType.V1I, "maxParallaxSteps"),
                    Variable(GLSLType.V2F, "uv", VariableMode.INOUT)
                ),
                concatDefines(key).toString() +
                        discardByCullingPlane +
                        normalTanBitanCalculation +
                        "if (parallaxScale != 0.0) {\n" +
                        "   vec3 tsV = normalize(vec3(" +
                        "       dot(finalPosition, finalTangent), " + // x
                        "       dot(finalPosition, finalBitangent), " + // y
                        "       dot(finalPosition, finalNormal)" + // z
                        "   ));\n" +
                        "   uv = applyTiling(uv, parallaxTiling);\n" +
                        "   vec2 scale = vec2(parallaxScale, parallaxBias);\n" +
                        "   vec2 steps = vec2(minParallaxSteps, maxParallaxSteps);\n" +
                        "   uv = parallaxMapUVs(parallaxMap, uv, tsV, scale, steps);\n" +
                        // todo implement depth offset by parallax for self-shadowing
                        "   uv = undoTiling(uv, parallaxTiling);\n" +
                        "   if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) discard;\n" +
                        "}\n" +
                        baseColorCalculation +
                        createColorFragmentStage()
            ).add(getReflectivity).add(applyTiling).add(parallaxMapping)
        )
    }
}