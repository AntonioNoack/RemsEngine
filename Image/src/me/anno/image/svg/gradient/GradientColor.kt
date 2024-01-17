package me.anno.image.svg.gradient

import me.anno.utils.Color.toHexColor

data class GradientColor(val color: Int, val percentage: Float) {
    override fun toString() = "(${color.toHexColor()} @$percentage)"
}