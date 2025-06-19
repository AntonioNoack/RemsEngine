package me.anno.tests.gfx.textures

import me.anno.engine.OfficialExtensions
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureCache
import me.anno.tests.gfx.testTexture
import me.anno.tests.image.createUVCheckerImage

fun main() {
    OfficialExtensions.initForTests()
    val largeImage = createUVCheckerImage().scaleUp(6, 6) // 700x6 ~ 4200
    testTexture("MipmapGen", false) {
        val texture = TextureCache[largeImage.ref].waitFor() as Texture2D
        // lies to force regeneration to test its performance
        texture.hasMipmap = false
        texture.filtering = Filtering.TRULY_NEAREST
        texture.bind(0, Filtering.LINEAR, Clamping.CLAMP) // ensure mipmaps
        texture
    }
}