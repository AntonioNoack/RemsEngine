package me.anno.tests.gfx.textures

import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.texture.Texture2D
import me.anno.image.raw.IntImage
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.files.UVChecker
import kotlin.concurrent.thread

fun main() {
    // check whether this works... -> ofc it doesn't
    // "No context is current or a function that is not available in the current context was called"
    // separate thread to upload textures...
    val src = UVChecker.value
    val image = src.readImage() as IntImage
    val callOnce = lazy {
        val texture = Texture2D("test", image.width, image.height, 1)
        texture.ensurePointer()
        thread { texture.createRGBA(image.data, false) }
        texture
    }
    testDrawing("Async Texture") {
        it.clear()
        val texture = callOnce.value
        if (texture.isCreated) {
            drawTexture(it.x, it.y, it.width, it.height, texture)
        }
    }
}