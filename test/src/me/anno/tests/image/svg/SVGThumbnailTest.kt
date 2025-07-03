package me.anno.tests.image.svg

import me.anno.engine.OfficialExtensions
import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.image.thumbs.ThumbnailCache
import me.anno.io.files.FileReference
import me.anno.io.files.inner.temporary.InnerTmpTextFile
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLWriter
import me.anno.jvm.HiddenOpenGLContext
import me.anno.maths.Maths.mix
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.toHexColor
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class SVGThumbnailTest {
    companion object {

        val baseline = IntImage(
            3, 2, intArrayOf(
                0xff0000, 0xffff00, 0x00ff00,
                0x000000, 0x0000ff, 0x00ffff
            ), false
        )

        fun createSVGFile(image: Image, scale: Int): FileReference {
            val svg = XMLNode("svg")
            val ws = image.width * scale
            val hs = image.height * scale
            svg["width"] = ws.toString()
            svg["height"] = hs.toString()
            svg["viewBox"] = "0 0 $ws $hs"
            val scaleStr = scale.toString()
            image.forEachPixel { x, y ->
                // <rect width="200" height="100" x="10" y="10" rx="20" ry="20" fill="blue" />
                val rect = XMLNode("rect")
                rect["width"] = scaleStr
                rect["height"] = scaleStr
                rect["x"] = (x * scale).toString()
                rect["y"] = (y * scale).toString()
                rect["fill"] = image.getRGB(x, y).toHexColor()
                svg.children.add(rect)
            }
            val svgText = XMLWriter.write(svg, null, false, false)
            return InnerTmpTextFile(svgText, "svg")
        }
    }

    @Test
    fun testPureThumbnail() {
        OfficialExtensions.initForTests()
        HiddenOpenGLContext.createOpenGL()

        val scale = 8
        val file = createSVGFile(baseline, scale)
        val thumbnail = ThumbnailCache.getEntry(file, baseline.width * scale).waitFor()!!
            .createImage(flipY = false, withAlpha = false)
        baseline.forEachPixel { x, y ->
            val baseColor = baseline.getRGB(x, y)
            val thumbColor = thumbnail.getRGB(
                ((x + 0.5f) * thumbnail.width).toInt() / baseline.width,
                ((y + 0.5f) * thumbnail.height).toInt() / baseline.height
            )
            assertEquals(baseColor, thumbColor)
        }
    }

    @Test
    fun testMeshThumbnail() {
        OfficialExtensions.initForTests()
        HiddenOpenGLContext.createOpenGL()

        val scale = 8
        val file0 = createSVGFile(baseline, scale)
        val file = file0.getChild("Scene.json")
        val thumbnail = ThumbnailCache.getEntry(file, 256).waitFor()!!
            .createImage(flipY = false, withAlpha = false)
        // thumbnail.write(desktop.getChild("svg3.png"))
        baseline.forEachPixel { x, y ->
            val baseColor = baseline.getRGB(x, y)
            val u = (x + 0.5f) / baseline.width
            val v = (y + 0.5f) / baseline.height
            val sc = 0.7f // thumbnail is rotated slightly, because it's showing a 3D mesh
            val thumbColor = thumbnail.getRGB(
                (mix(0.5f, u, sc) * thumbnail.width).toInt(),
                (mix(0.5f, v, sc) * thumbnail.height).toInt()
            )
            val threshold = 70 // shading changes the colors a bit
            // println("${baseColor.toHexColor()} vs ${thumbColor.toHexColor()}")
            assertEquals(baseColor.r(), thumbColor.r(), threshold)
            assertEquals(baseColor.g(), thumbColor.g(), threshold)
            assertEquals(baseColor.b(), thumbColor.b(), threshold)
        }
    }
}