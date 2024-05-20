package me.anno.mesh.blender.impl.mesh

import me.anno.mesh.blender.ConstructorData
import me.anno.mesh.blender.impl.BlendData

class MVert(ptr: ConstructorData) : BlendData(ptr) {

    // they stay the same inside a file
    private val coOffset = getOffset("co[3]")
    val noOffset = getOffset("no[3]") // no longer available with Blender 3.0+ :/

    val x get() = f32(coOffset)
    val y get() = f32(coOffset + 4)
    val z get() = f32(coOffset + 8)

    val nx get() = i16(noOffset) / 32767f
    val ny get() = i16(noOffset + 2) / 32767f
    val nz get() = i16(noOffset + 4) / 32767f

    override fun toString(): String {
        return if (noOffset >= 0) {
            "MVert { ($x,$y,$z), ($nx,$ny,$nz) }"
        } else {
            "MVert { ($x,$y,$z) }"
        }
    }
}