package me.anno.objects.lists

import me.anno.objects.Transform
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import org.joml.Vector4fc

class FlexibleList : Transform() {

    lateinit var list: ElementList
    lateinit var generator: ElementGenerator

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4fc) {
        list.onDraw(stack, time, color, generator) { super.onDraw(stack, time, color) }
    }

}