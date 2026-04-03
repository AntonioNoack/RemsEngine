package me.anno.ecs.components.mesh.material.shaders

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RendererLib
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.renderer.Renderer

object MatCapShader : ECSMeshShader("MatCap") {

    val matcap = """
        vec2 matcap(vec3 eye, vec3 normal) {
            vec3 reflected = reflect(eye, normal);
            float m = 2.8284271247461903 * sqrt( reflected.z+1.0 );
            return reflected.xy / m + 0.5;
        }
    """.trimIndent()

    override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        super.bind(shader, renderer, instanced)
        shader.v3f("cameraDirection", RenderState.cameraDirection)
    }

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return listOf(
            ShaderStage(
                "material",
                createFragmentVariables(key)
                    .filter { it.name != "uv" && it.name != "diffuseBase" } +
                        listOf(Variable(GLSLType.V3F, "cameraDirection")),
                discardByCullingPlane +

                        "finalNormal = normalize(gl_FrontFacing ? normal : -normal);\n" +
                        "vec2 uv = matcap(cameraDirection, finalNormal);\n" +

                        "vec4 texDiffuseMap = texture(diffuseMap, uv, lodBias);\n" +
                        "vec4 color = vec4(vertexColor0.rgb, 1.0) * texDiffuseMap;\n" +
                        "if(color.a < ${1f / 255f}) discard;\n" +
                        "finalColor = vec3(0.0);\n" +
                        "finalEmissive = color.rgb * 5.0;\n" +
                        "finalAlpha = color.a;\n" +
                        finalMotionCalculation
            ).add(RendererLib.getReflectivity).add(matcap)
        )
    }
}