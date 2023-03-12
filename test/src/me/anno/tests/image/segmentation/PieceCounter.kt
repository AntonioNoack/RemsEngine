package me.anno.tests.image.segmentation

import me.anno.Engine
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.ComputeTextureMode
import me.anno.gpu.texture.Texture2D
import me.anno.image.ImageGPUCache
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import org.joml.AABBf
import org.joml.Vector2i

fun main() {
    // inspired by https://www.invigon.de/de/products/stueckzaehlung,
    // let's count a few pieces :3
    HiddenOpenGLContext.createOpenGL()
    val source = downloads.getChild("cp_screenshot_bad.png")
    val image = ImageGPUCache[source, false]!!
    val groundTruth = AABBf(
        148f, 102f, 0f,
        148 + 23f, 102f + 20f, 0f
    )
    // -> try maybe 16 variations?
    val shader = ComputeShader(
        "correlation", Vector2i(16, 16), "" +
                "uniform int x0, x1, y0, y1;\n" +
                "uniform float scale;\n" +
                "layout(rgba8, binding = 0) uniform image2D src;\n" +
                "layout(r32f, binding = 1) uniform image2D dst;\n" +
                "void main(){\n" +
                "   ivec2 uv = ivec2(gl_GlobalInvocationID.xy);\n" +
                "   if(any(greaterThanEqual(uv + ivec2(x1-x0,y1-y0), imageSize(src)))) return;\n" +
                "   float corr = 0.0;\n" +
                "   for(int y=y0;y<y1;y++){\n" +
                "       for(int x=x0;x<x1;x++){\n" +
                "           corr += (imageLoad(src, ivec2(x,y)).x - 0.5) * (imageLoad(src, ivec2(uv.x+x-x0,uv.y+y-y0)).x - 0.5);\n" +
                "       }\n" +
                "   }\n" +
                "   corr *= scale;\n" +
                "   imageStore(dst, uv, vec4(corr));\n" +
                "}\n"
    )
    val workW = image.w - groundTruth.deltaX().toInt()
    val workH = image.h - groundTruth.deltaY().toInt()
    val dst = Texture2D("dst", workW, workH, 1)
    dst.create(TargetType.FloatTarget1)
    shader.use()
    shader.v1i("x0", groundTruth.minX.toInt())
    shader.v1i("x1", groundTruth.maxX.toInt())
    shader.v1i("y0", groundTruth.minY.toInt())
    shader.v1i("y1", groundTruth.maxY.toInt())
    shader.v1f("scale", 4f / (groundTruth.deltaX() * groundTruth.deltaY()))
    shader.bindTexture(0, image, ComputeTextureMode.READ)
    shader.bindTexture(1, dst, ComputeTextureMode.WRITE)
    shader.runBySize(workW, workH)
    dst.write(desktop.getChild("corr.png"))
    // to do we'd have to find the maxima in the correlation
    Engine.requestShutdown()
}