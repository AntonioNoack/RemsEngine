package me.anno.tests.gfx

import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawCurves
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawRounded
import me.anno.gpu.drawing.DrawStriped
import me.anno.gpu.drawing.DrawTextures
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.drawing.GFXx3D
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.TextureLib.depthTexture
import me.anno.gpu.texture.TextureLib.whiteCube
import me.anno.gpu.texture.TextureLib.whiteTex2da
import me.anno.gpu.texture.TextureLib.whiteTex3d
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.jvm.HiddenOpenGLContext
import me.anno.ui.UIColors
import me.anno.utils.Color.black
import me.anno.utils.Color.white
import me.anno.utils.Color.white4
import org.joml.Matrix4fArrayList
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DrawShaderCompileTest {

    @BeforeEach
    fun init() {
        HiddenOpenGLContext.createOpenGL()
    }

    @AfterEach
    fun finish() {
        GFX.check()
    }

    @Test
    fun testRectangleShaders() {
        DrawRectangles.drawRect(0, 0, 10, 10, -1)
        DrawRectangles.drawBorder(0, 0, 10, 10, -1, 10)
    }

    @Test
    fun testRectangleShadersBatch() {
        val batch = DrawRectangles.startBatch()
        DrawRectangles.drawRect(0, 0, 10, 10, -1)
        DrawRectangles.drawBorder(0, 0, 10, 10, -1, 10)
        DrawRectangles.finishBatch(batch)
    }

    @Test
    fun testLines() {
        DrawCurves.drawLine(0f, 0f, 10f, 10f, 1f, 1, black, false)
    }

    @Test
    fun testLinesBatch() {
        val batch = DrawCurves.lineBatch.start()
        DrawCurves.drawLine(0f, 0f, 10f, 10f, 1f, 1, black, false)
        DrawCurves.lineBatch.finish(batch)
    }

    @Test
    fun testQuadratic() {
        DrawCurves.drawQuadraticBezier(0f, 0f, 1f, 20f, 10f, 10f, 1f, 1, black, false)
    }

    @Test
    fun testCubic() {
        DrawCurves.drawCubicBezier(
            0f, 0f, 1f, 20f, 20f, 5f,
            10f, 10f, 1f, 1, black, false
        )
    }

    @Test
    fun testQuart() {
        DrawCurves.drawQuartBezier(
            0f, 0f, 1f, 20f, 20f, 5f,
            70f, 7f, 10f, 10f, 1f, 1, black, false
        )
    }

    @Test
    fun testTexture() {
        DrawTextures.drawTexture(0, 0, 10, 10, whiteTexture)
    }

    @Test
    fun testTexture3dSlice() {
        DrawTextures.draw3dSlice(
            0, 0, 10, 10, 0.5f,
            whiteTex3d, false, -1,
            applyToneMapping = false, showDepth = false
        )
    }

    @Test
    fun testTexture2dArraySlice() {
        DrawTextures.draw2dArraySlice(
            0, 0, 10, 10, 5,
            whiteTex2da, false, -1,
            applyToneMapping = false, showDepth = false
        )
    }

    @Test
    fun testTextureTransparentBackground() {
        DrawTextures.drawTransparentBackground(0, 0, 10, 10, 5f)
    }

    @Test
    fun testDrawProjection() {
        DrawTextures.drawProjection(
            0, 0, 10, 10,
            whiteCube, false, -1, applyToneMapping = false, showDepth = false
        )
    }

    @Test
    fun testDrawDepth() {
        DrawTextures.drawDepthTexture(0, 0, 10, 10, depthTexture)
    }

    @Test
    fun testDrawDepthArray() {
        DrawTextures.drawDepthTextureArray(0, 0, 10, 10, whiteTex2da, 0.5f)
    }

    @Test
    fun testDrawAlpha() {
        DrawTextures.drawTextureAlpha(0, 0, 10, 10, whiteTexture, -1)
    }

    @Test
    fun testDrawTextureArray() {
        DrawTextures.drawTextureArray(0, 0, 10, 0, whiteTex2da, 1f, false)
    }

    @Test
    fun testStriped() {
        DrawStriped.drawRectStriped(0, 0, 10, 10, 5, 7, -1)
        DrawStriped.drawRectStriped(0, 0, 10, 10, 5, 7, white)
    }

    @Test
    fun testRoundedRectangle() {
        DrawRounded.drawRoundedRect(
            0, 0, 10, 10, 1f, 1f, 1f, 1f, 2f,
            -1, UIColors.axisXColor, black, 1f
        )
    }

    // todo draw GPUFrame
    // todo draw texts
    // todo draw circles

    @Test
    fun testCircle2d() {
        GFXx2D.drawCircle(0, 0, 10f, 10f, 0f, UIColors.axisXColor, -1, black, 1f)
    }

    @Test
    fun testCircle3d() {
        val stack = Matrix4fArrayList()
        GFXx3D.draw3DCircle(stack, 0f, 0f, 360f, white4)
    }

    @Test
    fun testPlane3d() {
        val stack = Matrix4fArrayList()
        GFXx3D.draw3DPlanar(stack, whiteTexture, 10, 10, -1, Filtering.LINEAR, Clamping.CLAMP, white4)
    }

    @Test
    fun testArrow() {
        GFXx2D.drawHalfArrow(0f, 0f, 10f, 10f, -1, black)
    }
}