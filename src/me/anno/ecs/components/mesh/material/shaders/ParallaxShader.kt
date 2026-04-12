package me.anno.ecs.components.mesh.material.shaders

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.gpu.GFXState
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.DepthTransforms.depthToPosition
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.applyTiling
import me.anno.gpu.shader.ShaderLib.parallaxMapping
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.utils.types.Booleans.hasFlag

/**
 * Implements Silhouette Parallax Occlusion Mapping for typical PBR materials
 * */
object ParallaxShader : ECSMeshShader("parallax") {

    override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        super.bind(shader, renderer, instanced)

        val target = GFXState.currentBuffer
        shader.v2f("renderSize", target.width.toFloat(), target.height.toFloat())

        DepthTransforms.bindDepthUniforms(shader)
    }

    override fun loadVertex(key: ShaderKey, flags: Int): List<ShaderStage> {
        val vertexData = key.vertexData
        return vertexData.loadPosition +
                vertexData.loadNorTan + // always needed (normals and tangents)
                vertexData.loadColors + // always needed (uvs)
                f(vertexData.loadMotionVec, flags.hasFlag(NEEDS_MOTION_VECTORS))
    }

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
                    Variable(GLSLType.V1B, "parallaxSilhouette"),
                    Variable(GLSLType.V2F, "uv", VariableMode.INOUT),
                    Variable(GLSLType.V3F, "finalPosition"),
                    Variable(GLSLType.M4x4, "transform")
                ) + depthVars,
                concatDefines(key).toString() +
                        discardByCullingPlane +
                        normalTanBitanCalculation +
                        "if (parallaxScale != 0.0) {\n" +
                        // todo this is incorrect for directional lights :(
                        "   vec3 wsViewDir = rawCameraDirection(gl_FragCoord.xy / renderSize);\n" +
                        "   wsViewDir = normalize(wsViewDir);\n" +
                        "   vec3 texSpaceViewDir = normalize(vec3(\n" +
                        "       dot(wsViewDir, finalTangent), " + // x
                        "       dot(wsViewDir, finalBitangent), " + // y
                        "       dot(wsViewDir, finalNormal)" + // z
                        "   ));\n" +
                        "   uv = applyTiling(uv, parallaxTiling);\n" +
                        "   vec2 scale = vec2(parallaxScale, parallaxBias);\n" +
                        "   vec2 steps = vec2(minParallaxSteps, maxParallaxSteps);\n" +
                        "   float depthOffset = 0.0;\n" +
                        "   uv = parallaxMapUVs(parallaxMap, uv, texSpaceViewDir, scale, steps, depthOffset);\n" +

                        // uv transforms
                        "   uv = undoTiling(uv, parallaxTiling);\n" +
                        "   if (parallaxSilhouette && (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0)) discard;\n" +

                        // depth adjustments
                        "   if (!(abs(depthOffset) < 10.0 * scale.x)) depthOffset = 0.0;\n" +
                        "   vec4 newVertex = vec4(finalPosition - finalNormal * depthOffset, 1.0);\n" +
                        "   newVertex = matMul(transform, newVertex);\n" +
                        "   gl_FragDepth = newVertex.z/newVertex.w;\n" +
                        "}\n" +
                        baseColorCalculation +
                        (if (key.flags.hasFlag(NEEDS_COLORS)) {
                            createColorFragmentStage()
                        } else "")
            ).add(getReflectivity).add(applyTiling).add(parallaxMapping)
                .add(rawToDepth).add(depthToPosition)
        )
    }
}
