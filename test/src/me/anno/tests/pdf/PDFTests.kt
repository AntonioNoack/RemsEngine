package me.anno.tests.pdf

import me.anno.engine.OfficialExtensions
import me.anno.image.Image
import me.anno.image.ImageCache
import me.anno.image.thumbs.Thumbs
import me.anno.io.files.inner.temporary.InnerTmpTextFile
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.assertions.assertEquals
import org.joml.Vector4f
import org.junit.jupiter.api.Test

class PDFTests {

    val samplePDF = """
        %PDF-1.2 
        9 0 obj
        <<
        >>
        stream
        BT/ 9 Tf(Test)' ET
        endstream
        endobj
        4 0 obj
        <<
        /Type /Page
        /Parent 5 0 R
        /Contents 9 0 R
        >>
        endobj
        5 0 obj
        <<
        /Kids [4 0 R ]
        /Count 1
        /Type /Pages
        /MediaBox [ -2 -2 21 10 ]
        >>
        endobj
        3 0 obj
        <<
        /Pages 5 0 R
        /Type /Catalog
        >>
        endobj
        trailer
        <<
        /Root 3 0 R
        >>
        %%EOF
    """.trimIndent()

    @Test
    fun testPDFReadingAsImage() {

        OfficialExtensions.initForTests()

        val testFile = InnerTmpTextFile(samplePDF)
        val px = ImageCache[testFile.getChild("256px.png"), false]!!
        assertEquals(256, px.width)
        assertEquals(12 * 256 / 23, px.height) // 12 x 23 is the dimensions of the media box
        checkColors(px)
    }

    @Test
    fun testPDFThumbnail() {
        OfficialExtensions.initForTests()
        HiddenOpenGLContext.createOpenGL()

        val testFile = InnerTmpTextFile(samplePDF)
        val tex = Thumbs[testFile.getChild("256px.png"), 256, false]!!
        assertEquals(256, tex.width)
        assertEquals(12 * 256 / 23, tex.height) // 12 x 23 is the dimensions of the media box

        checkColors(tex.createImage(flipY = false, withAlpha = false))
    }

    private fun checkColors(px: Image) {
        // check the color (min,max,avg)
        val minColor = Vector4f(Float.POSITIVE_INFINITY)
        val maxColor = Vector4f(Float.NEGATIVE_INFINITY)
        val avgColor = Vector4f()
        val tmp = Vector4f()
        px.forEachPixel { x, y ->
            val rgb = px.getRGB(x, y)
            rgb.toVecRGBA(tmp)
            minColor.min(tmp)
            maxColor.max(tmp)
            avgColor.add(tmp)
        }
        avgColor.div((px.width * px.height).toFloat())
        assertEquals(Vector4f(0f, 0f, 0f, 1f), minColor)
        assertEquals(Vector4f(1f, 1f, 1f, 1f), maxColor)
        // idk about checking that...
        assertEquals(Vector4f(0.87f, 0.87f, 0.87f, 1f), avgColor, 0.05)
    }
}