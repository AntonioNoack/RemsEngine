package me.anno.gpu.shader

import org.joml.Matrix4x3f
import org.joml.Vector4f

object YUVHelper {

    val YUV_Y = Vector4f(0.299f, 0.587f, 0.114f, 0f)
    val YUV_U = Vector4f(-0.169f, -0.331f, 0.500f, 0.5f)
    val YUV_V = Vector4f(0.500f, -0.419f, -0.081f, 0.5f)

    val yuvMatrix = Matrix4x3f(
        YUV_Y.x, YUV_U.x, YUV_V.x,
        YUV_Y.y, YUV_U.y, YUV_V.y,
        YUV_Y.z, YUV_U.z, YUV_V.z,
        YUV_Y.w, YUV_U.w, YUV_V.w,
    )

    val yuvMatrixInv = yuvMatrix.invert(Matrix4x3f())

    val rgb2yuv = "" +
            "vec3 rgb2yuv(vec3 rgb){\n" +
            "   vec4 rgba = vec4(rgb,1);\n" +
            "   return vec3(\n" +
            "       dot(rgb,  vec3(${YUV_Y.x}, ${YUV_Y.y}, ${YUV_Y.z})),\n" +
            "       dot(rgba, vec4(${YUV_U.x}, ${YUV_U.y}, ${YUV_U.z}, 0.5)),\n" +
            "       dot(rgba, vec4(${YUV_V.x}, ${YUV_V.y}, ${YUV_V.z}, 0.5))\n" +
            "   );\n" +
            "}\n"

    val rgb2uv = "" +
            "vec2 RGBtoUV(vec3 rgb){\n" +
            "   vec4 rgba = vec4(rgb,1.0);\n" +
            "   return vec2(\n" +
            "       dot(rgba, vec4(${YUV_U.x}, ${YUV_U.y}, ${YUV_U.z}, 0.5)),\n" +
            "       dot(rgba, vec4(${YUV_V.x}, ${YUV_V.y}, ${YUV_V.z}, 0.5))\n" +
            "   );\n" +
            "}\n"

    val yuv2rgb = "" +
            "vec3 yuv2rgb(vec3 yuv){\n" +
            "   return vec3(\n" +
            "       dot(yuv, vec3(${yuvMatrixInv.m00}, ${yuvMatrixInv.m10}, ${yuvMatrixInv.m20}))+${yuvMatrixInv.m30},\n" +
            "       dot(yuv, vec3(${yuvMatrixInv.m01}, ${yuvMatrixInv.m11}, ${yuvMatrixInv.m21}))+${yuvMatrixInv.m31},\n" +
            "       dot(yuv, vec3(${yuvMatrixInv.m02}, ${yuvMatrixInv.m12}, ${yuvMatrixInv.m22}))+${yuvMatrixInv.m32}\n" +
            "   );\n" +
            "}\n"

}