package me.anno.gpu.shader

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFX
import me.anno.gpu.shader.builder.Variable
import me.anno.utils.pooling.JomlPools
import org.joml.Matrix4f
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * helps loading and storing depth values in shaders from textures
 * */
object DepthTransforms {

    val rawToDepth = "" +
            "#ifndef RAW_TO_DEPTH\n" +
            "#define RAW_TO_DEPTH\n" +
            "float rawToDepth(float rawDepth) {\n" +
            // todo depth is offset... why???
            // for now, this is correct, confirmed by DEPTH_TEST
            "   if(d_near < 0.0) return rawDepth * 2.0 - 1.0;\n" +  // orthographic
            "   if(reverseDepth) return d_near / max(rawDepth, 0.0);\n" + // perspective, reverse-depth
            "   else             return 2.0 * d_near / max(1.0 - rawDepth, 0.0);\n" + // perspective, normal depth
            "}\n" +
            "float depthToRaw(float depth) {\n" +
            // todo depth is offset... why???
            "   if(d_near < 0.0) return depth * 0.5 + 0.5;\n" +  // orthographic
            "   if(reverseDepth) return d_near / max(depth, 0.0);\n" + // perspective, reverse-depth
            "   else      return 1.0 - (d_near / max(0.5 * depth, 0.0));\n" + // perspective, normal depth
            "}\n" +
            "#endif\n"

    val depthToPosition = "" +
            "#ifndef DEPTH_TO_POS\n" +
            "#define DEPTH_TO_POS\n" +
            "vec3 quatRot(vec3,vec4);\n" +
            "float rawToDepth(float);\n" +
            "vec3 rawCameraDirection(vec2 uv) {\n" +
            "   if(d_near < 0.0) return d_camRot.xyz;\n" + // orthographic
            // else perspective
            //   the traditional way, which misses a eyeOffset/near somewhere though
            //   d_fovFactor: 2 * tan(fovX/YRadians * 0.5)
            // "   return quatRot(vec3((uv-d_uvCenter)*d_fovFactor, -1.0), d_camRot);\n" +
            //   the easy way, which works for VR, too:
            "   vec4 pos = matMul(d_cameraMatrixInv, vec4(uv*2.0-1.0, d_near, 1.0));\n" +
            "   return pos.xyz/pos.w;\n" +
            "}\n" +
            "vec3 depthToPosition(vec2 uv, float depth){\n" + // position is in camera space, so camera is at zero
            "   if(d_near < 0.0) return matMul(d_orthoMat, vec4(uv*2.0-1.0,depth,1.0));\n" + // orthographic
            "   return rawCameraDirection(uv) * depth;\n" + // perspective
            "}\n" +
            "vec3 getLocalCameraPosition(vec2 uv){\n" +
            "   if(d_near < 0.0) return matMul(d_orthoMat, vec4(uv*2.0-1.0,0.0,1.0));\n" + // orthographic
            "   return vec3(0.0);\n" + // perspective;
            "}\n" +
            "vec3 rawDepthToPosition(vec2 uv, float rawDepth){ return depthToPosition(uv, rawToDepth(rawDepth)); }\n" +
            "#endif\n"

    val depthVars = listOf(
        Variable(GLSLType.V4F, "d_camRot"),
        Variable(GLSLType.V1F, "d_near"),
        Variable(GLSLType.V2F, "d_uvCenter"),
        Variable(GLSLType.M4x3, "d_orthoMat"),
        Variable(GLSLType.V1B, "reverseDepth"),
        Variable(GLSLType.M4x4, "d_cameraMatrixInv")
    )

    fun bindDepthUniforms(shader: GPUShader) {
        bindDepthUniforms(shader, RenderState.cameraDirection, RenderState.cameraRotation, RenderState.cameraMatrixInv)
    }

    private val tmpMatrix = Matrix4f()
    fun bindDepthUniforms(
        shader: GPUShader,
        cameraDirection: Vector3f,
        cameraRotation: Quaternionf,
        cameraMatrixInv: Matrix4f
    ) {
        if (!RenderState.isPerspective) {
            // orthogonal
            val dir = cameraDirection
            shader.v4f("d_camRot", dir.x, dir.y, dir.z, 1f)
            shader.v1f("d_near", -1f)
            // a matrix that transforms uv[-1,+1] x depth[0,1] into [left,right] x [top,bottom] x [near,far]
            shader.m4x3("d_orthoMat", JomlPools.mat4x3f.borrow().set(cameraMatrixInv))
            shader.v1b("reverseDepth", GFX.supportsClipControl)
            shader.v2f("d_uvCenter", 0f, 0f) // idk yet
        } else {
            // perspective
            shader.v1b("reverseDepth", GFX.supportsClipControl)
            shader.v4f("d_camRot", cameraRotation)
            shader.v2f("d_uvCenter", RenderState.fovXCenter, RenderState.fovYCenter)
            shader.v1f("d_near", RenderState.near)
            if (GFX.supportsClipControl) {
                shader.m4x4("d_cameraMatrixInv", cameraMatrixInv)
            } else {
                // why is that required in the first place?
                val scale = 2f * RenderState.near
                tmpMatrix.set(cameraMatrixInv)
                tmpMatrix.m03 *= scale
                tmpMatrix.m13 *= scale
                tmpMatrix.m23 *= scale
                tmpMatrix.m33 *= scale
                shader.m4x4("d_cameraMatrixInv", tmpMatrix)
            }
        }
    }
}