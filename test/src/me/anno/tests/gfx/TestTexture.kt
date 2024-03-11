package me.anno.tests.gfx

import me.anno.config.DefaultConfig
import me.anno.gpu.texture.ITexture2D
import me.anno.ui.base.image.ImagePanel
import me.anno.ui.debug.TestEngine

fun testTexture(title: String, flipY: Boolean, draw: (p: ImagePanel) -> ITexture2D) {
    TestEngine.testUI3(title) {
        object : ImagePanel(DefaultConfig.style) {
            override fun getTexture() = draw(this)
        }.apply {
            // it's a debug panel, so make it movable
            allowMovement = true
            allowZoom = true
            minZoom = 1e-3f // allow zooming out 1000x
            this.flipY = flipY
            showAlpha = true
        }
    }
}