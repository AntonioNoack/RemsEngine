package me.anno.ecs.components.mesh.material.shaders

import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.renderer.Renderer
import org.joml.Quaternionf
import kotlin.math.sqrt

object MatCapShader : ECSMeshShader("MatCap") {

    val matcap = """
        vec2 matcap(vec3 normal) {
            float m = ${sqrt(8f)} * sqrt(normal.z + 1.0);
            return normal.xy / m + 0.5;
        }
    """.trimIndent()

    var maxBrightness = 20f

    fun bindCameraRotation(shader: Shader) {
        val ry = RenderState.cameraRotation.getEulerAngleYXZvY()
        shader.v4f("cameraRotationXZ", Quaternionf().rotationY(ry))
    }

    override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        super.bind(shader, renderer, instanced)
        bindCameraRotation(shader)
    }

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return listOf(
            ShaderStage(
                "material",
                createFragmentVariables(key)
                    .filter { it.name != "uv" && it.name != "diffuseBase" } + listOf(
                    Variable(GLSLType.V4F, "cameraRotationXZ")
                ), discardByCullingPlane +

                        "finalNormal = normalize(gl_FrontFacing ? normal : -normal);\n" +
                        "vec3 relNormal = quatRotInv(finalNormal, cameraRotationXZ);\n" +
                        "vec2 uv = matcap(relNormal);\n" +

                        "vec4 texDiffuseMap = texture(diffuseMap, uv, lodBias);\n" +
                        "vec4 color = vec4(vertexColor0.rgb, 1.0) * texDiffuseMap;\n" +
                        "if(color.a < ${1f / 255f}) discard;\n" +
                        "finalColor = vec3(0.0);\n" +
                        "finalEmissive = color.rgb / (${1f + 1f / maxBrightness} - color.rgb);\n" + // SDR -> HDR
                        "finalAlpha = color.a;\n" +
                        finalMotionCalculation
            ).add(getReflectivity).add(quatRot).add(matcap)
        )
    }
}