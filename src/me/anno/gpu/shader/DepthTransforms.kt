package me.anno.gpu.shader

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.shader.builder.Variable
import me.anno.utils.pooling.JomlPools
import kotlin.math.abs
import kotlin.math.tan

object DepthTransforms {

    val rawToDepth = "" +
            "#ifndef RAW_TO_DEPTH\n" +
            "#define RAW_TO_DEPTH\n" +
            "float rawToDepth(float rawDepth){\n" +
            "   if(d_fovFactor.z < 0.0) return rawDepth;\n" +  // orthographic
            "   return d_fovFactor.z / rawDepth;\n" +
            "}\n" +
            "float depthToRaw(float depth){\n" +
            "   if(d_fovFactor.z < 0.0) return depth;\n" +  // orthographic
            "   return d_fovFactor.z / depth;\n" +
            "}\n" +
            "#endif\n"

    val depthToPosition = "" +
            "#ifndef DEPTH_TO_POS\n" +
            "#define DEPTH_TO_POS\n" +
            "vec3 quatRot(vec3,vec4);\n" +
            "float rawToDepth(float);\n" +
            "vec3 rawCameraDirection(vec2 uv){\n" +
            "   if(d_fovFactor.z < 0.0) return d_camRot.xyz;\n" + // orthographic
            "   return quatRot(vec3((uv-0.5)*d_fovFactor.xy, -1.0), d_camRot);\n" +
            "}\n" +
            "vec3 depthToPosition(vec2 uv, float depth){\n" + // position is in camera space, so camera is at zero
            "   if(d_fovFactor.z < 0.0) return matMul(d_orthoMat, vec4(uv*2.0-1.0,depth,1.0));\n" + // orthographic
            "   return rawCameraDirection(uv) * depth;\n" +
            "}\n" +
            "vec3 getLocalCameraPosition(vec2 uv){\n" +
            "   if(d_fovFactor.z < 0.0) return matMul(d_orthoMat, vec4(uv*2.0-1.0,0.0,1.0));\n" + // orthographic
            "   return vec3(0.0);\n" +
            "}\n" +
            "vec3 rawDepthToPosition(vec2 uv, float rawDepth){ return depthToPosition(uv, rawToDepth(rawDepth)); }\n" +
            "#endif\n"

    val depthVars = listOf(
        Variable(GLSLType.V4F, "d_camRot"),
        Variable(GLSLType.V3F, "d_fovFactor"),
        Variable(GLSLType.M4x3, "d_orthoMat"),
    )

    fun bindDepthToPosition(shader: Shader) {
        val mat0 = RenderState.cameraMatrix
        if (abs(mat0.m33 - 1f) < 0.001f) {
            // orthogonal
            val dir = RenderState.cameraDirection
            shader.v4f("d_camRot", dir.x.toFloat(), dir.y.toFloat(), dir.z.toFloat(), 1f)
            shader.v3f("d_fovFactor", 0f, 0f, -1f)
            // a matrix that transforms uv[-1,+1] x depth[0,1] into [left,right] x [top,bottom] x [near,far]
            shader.m4x3("d_orthoMat", JomlPools.mat4x3f.borrow().set(RenderState.cameraMatrixInv))
        } else {
            // perspective
            shader.v4f("d_camRot", RenderState.cameraRotation)
            shader.v3f(
                "d_fovFactor",
                2f * tan(RenderState.fovXRadians * 0.5f),
                2f * tan(RenderState.fovYRadians * 0.5f),
                RenderState.near
            )
        }
    }
}