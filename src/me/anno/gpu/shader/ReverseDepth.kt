package me.anno.gpu.shader

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.shader.builder.Variable
import kotlin.math.tan

object ReverseDepth {

    val depthToPosition = "" +
            "vec3 quatRot(vec3,vec4);\n" +
            "vec3 rawCameraDirection(){\n" +
            "   return quatRot(vec3((uv-0.5)*fovFactor.xy, -1.0), camRot);\n" +
            "}\n" +
            "vec3 depthToPosition(float depth){\n" +
            "   return rawCameraDirection() * (fovFactor.z / depth);\n" +
            "}\n"

    val depthToPositionList = listOf(
        Variable(GLSLType.V4F, "camRot"),
        Variable(GLSLType.V3F, "fovFactor")
    )

    fun bindDepthToPosition(shader: Shader) {
        shader.v4f("camRot", RenderState.cameraRotation)
        shader.v3f(
            "fovFactor",
            2f * tan(RenderState.fovXRadians * 0.5f),
            2f * tan(RenderState.fovYRadians * 0.5f),
            RenderState.near
        )
    }
}