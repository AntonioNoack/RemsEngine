package me.anno.tests.gfx.textures

import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.image.ImageGPUCache
import me.anno.tests.gfx.testTexture
import me.anno.utils.files.UVChecker

fun main() {
    val largeImage = UVChecker.value.image.scaleUp(6, 6) // 700x6 ~ 4200
    testTexture("MipmapGen", false) {
        val texture = ImageGPUCache[largeImage.ref, false]!!
        // lies to force regeneration to test its performance
        texture.hasMipmap = false
        texture.filtering = GPUFiltering.TRULY_NEAREST
        texture.bind(0, GPUFiltering.LINEAR, Clamping.CLAMP) // ensure mipmaps
        texture
    }
}