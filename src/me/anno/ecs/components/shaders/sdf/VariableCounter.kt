package me.anno.ecs.components.shaders.sdf

/**
 * generates a new variable index on request.
 * this prevents from variable names appearing twice and causing issues
 * */
class VariableCounter(private var value: Int = 0) {
    fun next() = value++
}