package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

/**
 * Used in newer Blender files for UVs
 * Struct vec2f(8): { x: float, y: float }
 * Struct vec2i(8): { x: int, y: int }
 * Struct vec2s(4): { x: short, y: short }
 * Struct vec3f(12): { x: float, y: float, z: float }
 * */
class BVector2f(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {
    val x get() = float("x")
    val y get() = float("y")

    override fun toString(): String {
        return "($x, $y)"
    }
}