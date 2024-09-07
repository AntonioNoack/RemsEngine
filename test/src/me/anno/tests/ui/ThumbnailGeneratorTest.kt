package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.image.thumbs.Thumbs
import me.anno.io.files.Reference.getReference
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.utils.ThumbnailPanel

fun main() {
    // todo bug: why are SDF-components not rendering properly in thumbnails???
    val src = getReference("C:/Users/Antonio/Documents/RemsEngine/YandereSim/SDF Pyramid.json")
    testUI3("ThumbnailGenTest", object: ThumbnailPanel(src, style){
        override fun onUpdate() {
            Thumbs.invalidate(src)
        }
    })
}