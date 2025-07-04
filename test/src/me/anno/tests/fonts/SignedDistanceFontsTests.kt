package me.anno.tests.fonts

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.components.text.SDFTextComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.RenderState
import me.anno.fonts.Font
import me.anno.fonts.FontManager
import me.anno.fonts.signeddistfields.TextSDFGroup
import me.anno.gpu.FinalRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.drawing.Perspective
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.texture.ITexture2D
import me.anno.image.Image
import me.anno.jvm.HiddenOpenGLContext
import me.anno.tests.FlakyTest
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.Color.black
import me.anno.utils.Color.g
import me.anno.utils.OS.desktop
import me.anno.utils.Sleep
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import me.anno.utils.hpc.ProcessingQueue
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.f1
import me.anno.utils.types.Floats.toRadians
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

/**
 * tests whether text rendered using SDFs matches that of the raw text textures
 * */
class SignedDistanceFontsTests {

    @Test
    fun testProcessingQueue() {
        val pq = ProcessingQueue("test")
        var ok = false
        pq += { ok = true }
        Sleep.waitUntil(true) { ok }
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testGetSDFTexture() {
        Engine.cancelShutdown()
        OfficialExtensions.initForTests()
        HiddenOpenGLContext.createOpenGL()
        val font = Font("Verdana", 40f)
        val roundCorners = false
        val codepoint = 83
        Sleep.waitUntilDefined(true) {
            val textSDF = TextSDFGroup.getTextSDF(font, codepoint, roundCorners)
            textSDF?.texture?.createdOrNull()
        }
    }

    @Test
    @FlakyTest
    @Execution(ExecutionMode.SAME_THREAD)
    fun testSignedDistanceFonts() {
        // todo bug: when running this together with the others,
        //  bySDFTexture is completely black... why?

        // todo bug: the text is very compressed on the x-axis... why???
        if (true) return

        Engine.cancelShutdown()
        OfficialExtensions.initForTests()
        HiddenOpenGLContext.createOpenGL()

        val text = "SDF Test"
        val font = Font("Verdana", 80f) // -> 181 x 50px
        // generate texture
        val baseTexture = FontManager.getTexture(font, text, -1, -1).waitFor()!!
        val baseImage = baseTexture.createImage(flipY = false, withAlpha = false)
        val sdfTextMesh = SDFTextComponent(text, font, AxisAlignment.CENTER)

        // render sdfTextMesh into texture
        val bySDFTexture = compToTexture(baseTexture.width, baseTexture.height, sdfTextMesh)
        val bySDFImage = bySDFTexture.createImage(flipY = false, withAlpha = false)
        // check that texture and SDF texture align properly
        checkRendering(baseImage, bySDFImage)
    }

    private fun checkRendering(baseImage: Image, bySDFImage: Image) {
        val matchMatrix = IntArray(4)
        val dx = 2
        val dy = -1
        baseImage.forEachPixel { x, y ->
            val i0 = isWhite(baseImage.getRGB(x, y))
            val i1 = isWhite(bySDFImage.getRGB(x + dx, y + dy))
            matchMatrix[i0.toInt() + i1.toInt(2)]++
        }
        baseImage.write(desktop.getChild("base.png"))
        bySDFImage.write(desktop.getChild("bySDF.png"))
        val size = (baseImage.width * baseImage.height).toFloat()
        println("Matches: ${matchMatrix.map { (it * 100f / size).f1() }}%, size: ${baseImage.width} x ${baseImage.height}")
        // we want 95% accuracy
        assertTrue(matchMatrix[0] + matchMatrix[3] >= size * 0.95f)
        // approximately 14% should be white, the remainder should be black
        assertEquals(matchMatrix[3] / size, 0.14f, 0.05f)
    }

    private fun isWhite(color: Int): Boolean {
        return color.g() > 127
    }

    private fun compToTexture(width: Int, height: Int, component: Component): ITexture2D {
        val framebuffer = Framebuffer("c2t", width, height, 1, TargetType.UInt8x4, DepthBufferType.NONE)
        val pipeline = Pipeline(null)
        // define render state
        RenderState.cameraPosition.set(0.0, 0.0, 1.0)
        Perspective.setPerspective(
            RenderState.cameraMatrix, (70f).toRadians(),
            width.toFloat() / height.toFloat(),
            0.01f, 100f, 0f, 0f, false
        )
        RenderState.cameraRotation.identity()
        RenderState.calculateDirections(true, true)
        RenderState.near = 0.01f
        RenderState.far = 100f
        RenderState.aspectRatio = width.toFloat() / height.toFloat()
        RenderState.viewIndex = 0
        pipeline.frustum.setToEverything(RenderState.cameraPosition, RenderState.cameraRotation)

        while (true) {
            val missing = FinalRendering.runFinalRendering {
                pipeline.clear()
                pipeline.fill(component)
            }
            if (missing == null) {
                break
            }
            Sleep.work(true)
            println("Waiting on $missing")
            Thread.sleep(5)
        }

        assertFalse(pipeline.defaultStage.isEmpty())
        useFrame(framebuffer) {
            framebuffer.clearColor(black)
            pipeline.defaultStage.bindDraw(pipeline)
        }
        return framebuffer.getTexture0()
    }
}