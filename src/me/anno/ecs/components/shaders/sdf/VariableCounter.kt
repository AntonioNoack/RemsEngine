package me.anno.ecs.components.shaders.sdf

class VariableCounter(private var value: Int = 0) {
    fun next() = value++
}