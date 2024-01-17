package me.anno.image.gimp

import me.anno.image.Image

class Layer(val width: Int, val height: Int, val name: String, val baseType: ImageType, val hasAlpha: Boolean) {
    var x = 0
    var y = 0
    var opacity = 255
    var blendSpace = 0
    var isVisible = true
    var image: Image? = null
}