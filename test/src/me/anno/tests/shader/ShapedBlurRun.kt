package me.anno.tests.shader

import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.effects.ShapedBlur
import me.anno.graph.render.Texture
import me.anno.graph.render.effects.ShapedBlurNode
import me.anno.image.ImageGPUCache
import me.anno.utils.OS.desktop
import me.anno.utils.OS.pictures

fun main() {
    HiddenOpenGLContext.createOpenGL()
    // Loading test: which filters are available?
    println(ShapedBlur.filters.keys)
    val node = ShapedBlurNode()
    node.type = "heart_4x32"
    node.setInput(1, ImageGPUCache[pictures.getChild("blurTest.png"), false]!!)
    node.executeAction()
    (node.getOutput(1) as Texture).tex
        .write(desktop.getChild("${node.type}.png"))
    // Serialization test:
    println(node)
}
