package me.anno.remsstudio.objects.lists

import me.anno.remsstudio.objects.Transform

class Element(val width: Float, val height: Float, val depth: Float, val transform: Transform){
    constructor(width: Int, height: Int, transform: Transform): this(width.toFloat(), height.toFloat(), 0f, transform)
}