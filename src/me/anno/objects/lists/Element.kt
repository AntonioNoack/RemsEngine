package me.anno.objects.lists

import me.anno.objects.Transform

class Element(val width: Float, val height: Float, val depth: Float, val transform: Transform){
    constructor(width: Int, height: Int, transform: Transform): this(width.toFloat(), height.toFloat(), 0f, transform)
}