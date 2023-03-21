package me.anno.gpu.shader

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.shader.builder.Variable
import kotlin.math.tan

object ReverseDepth {

    val rawToDepth = "" +
            "float rawToDepth(float rawDepth){\n" +
            "   return fovFactor.z / rawDepth;\n" +
            "}\n"

    val depthToPosition = "" +
            "vec3 quatRot(vec3,vec4);\n" +
            "float rawToDepth(float);\n" +
            "vec3 rawCameraDirection(vec2 uv){\n" +
            "   return quatRot(vec3((uv-0.5)*fovFactor.xy, -1.0), camRot);\n" +
            "}\n" +
            "vec3 rawDepthToPosition(vec2 uv, float rawDepth){\n" +
            "   return rawCameraDirection(uv) * rawToDepth(rawDepth);\n" +
            "}\n" +
            "vec3 depthToPosition(vec2 uv, float depth){\n" +
            "   return rawCameraDirection(uv) * depth;\n" +
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