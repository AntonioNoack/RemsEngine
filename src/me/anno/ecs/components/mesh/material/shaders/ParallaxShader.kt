package me.anno.ecs.components.mesh.material.shaders

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.engine.ui.render.Renderers.numStepsRenderer
import me.anno.gpu.GFXState
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.DepthTransforms.depthToPosition
import me.anno.gpu.shader.DepthTransforms.depthVars
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib.costShadingFunc
import me.anno.gpu.shader.ShaderLib.applyTiling
import me.anno.gpu.shader.ShaderLib.parallaxMapping
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Strings.iff

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
                        "int numSteps = 0;\n" +
                        "if (parallaxScale != 0.0) {\n" +
                        // todo this is slightly incorrect for directional lights :(
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
                        "   uv = parallaxMapUVs(parallaxMap, uv, texSpaceViewDir, scale, steps, depthOffset, numSteps);\n" +

                        // sample neighbors
                        // todo bug: normal fades from the side... and only in forward-rendering... why??
                        "   vec2 duv = 1.5 / vec2(textureSize(parallaxMap, 0));\n" +
                        "   float pX = texture(parallaxMap, uv + vec2(duv.x, 0.0), 0).r;\n" +
                        "   float pY = texture(parallaxMap, uv + vec2(0.0, duv.y), 0).r;\n" +
                        "   float nX = texture(parallaxMap, uv - vec2(duv.x, 0.0), 0).r;\n" +
                        "   float nY = texture(parallaxMap, uv - vec2(0.0, duv.y), 0).r;\n" +
                        // scale (match your parallaxScale!)
                        "   vec3 n_ts = normalize(vec3(\n" +
                        "       (nX - pX) * parallaxScale,\n" +
                        "       (nY - pY) * parallaxScale,\n" +
                        "       4.0\n" +
                        "   ));\n" +
                        "   mat3 tbn = mat3(finalTangent, finalBitangent, finalNormal);\n" +
                        "   vec3 displacedNormal = normalize(tbn * n_ts);\n" +

                        // uv transforms
                        "   uv = undoTiling(uv, parallaxTiling);\n" +
                        "   if (parallaxSilhouette && (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0)) discard;\n" +

                        if (false) {
                            // depth adjustments for self-shadowing -> not working yet :(
                            "" +
                                    "   if (!(abs(depthOffset) < 10.0 * scale.x)) depthOffset = 0.0;\n" +
                                    // I'm unsure about finalNormal being correct;
                                    "   finalPosition -= finalNormal * depthOffset;\n" +
                                    // replace or blend
                                    "   finalNormal = displacedNormal;\n" +
                                    // this part is correct:
                                    "   #define CUSTOM_DEPTH\n" +
                                    "   vec4 newVertex = matMul(transform, vec4(finalPosition, 1.0));\n" +
                                    "   gl_FragDepth = newVertex.z/newVertex.w;\n"
                        } else {
                            ""
                        } +
                        "}\n" +
                        baseColorCalculation +
                        (if (key.flags.hasFlag(NEEDS_COLORS)) {
                            createColorFragmentStage() +
                                    "finalColor = vec3(0.0); finalEmissive = costShadingFunc(float(numSteps)/float(maxParallaxSteps));\n"
                                        .iff(key.renderer == numStepsRenderer)
                        } else "")
            ).add(getReflectivity).add(applyTiling).add(parallaxMapping)
                .add(rawToDepth).add(depthToPosition).add(costShadingFunc)
        )
    }
}
