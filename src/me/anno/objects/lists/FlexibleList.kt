package me.anno.objects.lists

import me.anno.objects.Transform
import org.joml.Matrix4fArrayList
import org.joml.Vector4f

class FlexibleList : Transform() {

    lateinit var list: ElementList
    lateinit var generator: ElementGenerator

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {
        list.onDraw(stack, time, color, generator) { super.onDraw(stack, time, color) }
    }

}