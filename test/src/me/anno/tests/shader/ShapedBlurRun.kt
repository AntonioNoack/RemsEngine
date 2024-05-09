package me.anno.tests.shader

import me.anno.jvm.HiddenOpenGLContext
import me.anno.gpu.shader.effects.ShapedBlur
import me.anno.gpu.texture.TextureCache
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.effects.ShapedBlurNode
import me.anno.image.raw.IntImage
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha
import me.anno.utils.OS.desktop
import kotlin.random.Random

fun main() {
    HiddenOpenGLContext.createOpenGL()
    // Loading test: which filters are available?
    println(ShapedBlur.filters.keys)
    val node = ShapedBlurNode()
    node.type = "heart_4x32"
    val testImage = IntImage(1024, 1024, false)
    testImage.data.fill(black)
    val random = Random(1234)
    val ds = 20
    for (i in 0 until 100) {
        val color = random.nextInt(0xffffff).withAlpha(255)
        val x = random.nextInt(testImage.width - ds)
        val y = random.nextInt(testImage.height - ds)
        for (dy in 0 until ds) {
            for (dx in 0 until ds) {
                testImage.setRGB(x + dx, y + dy, color)
            }
        }
    }
    node.setInput(1, TextureCache[testImage.ref, false]!!)
    node.setInput(3, 4f) // gamma high, so small pixels can be blown up
    node.executeAction()
    (node.getOutput(1) as Texture).tex
        .write(desktop.getChild("${node.type}.png"))
    // Serialization test:
    println(node)
}
