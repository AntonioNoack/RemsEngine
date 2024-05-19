package me.anno.mesh.blender.impl.primitives

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BlendData

/**
 * Used in newer Blender files for UVs
 * Struct vec2f(8): { x: float, y: float }
 * Struct vec2i(8): { x: int, y: int }
 * Struct vec2s(4): { x: short, y: short }
 * Struct vec3f(12): { x: float, y: float, z: float }
 * */
class BVector2s(ptr: ConstructorData) : BlendData(ptr) {

    private val xOffset = getOffset("x")
    private val yOffset = getOffset("y")

    val x get() = short(xOffset)
    val y get() = short(yOffset)

    override fun toString(): String {
        return "($x, $y)"
    }
}