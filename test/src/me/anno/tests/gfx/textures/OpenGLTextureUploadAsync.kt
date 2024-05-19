package me.anno.tests.gfx.textures

import me.anno.engine.OfficialExtensions
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.invisibleTexture
import me.anno.tests.gfx.testTexture
import me.anno.tests.image.createUVCheckerImage
import kotlin.concurrent.thread

fun main() {
    // check whether this works... -> ofc it doesn't
    // "No context is current or a function that is not available in the current context was called"
    // separate thread to upload textures...
    OfficialExtensions.initForTests()
    val image = createUVCheckerImage()
    val callOnce = lazy {
        val texture = Texture2D("test", image.width, image.height, 1)
        texture.ensurePointer()
        thread { texture.createRGBA(image.data, false) }
        texture
    }
    testTexture("Async Texture", false) {
        val texture = callOnce.value
        if (texture.wasCreated) {
            texture
        } else invisibleTexture
    }
}