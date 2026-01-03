package me.anno.mesh.blender

class DNAType(val name: String, val sizeInBytes: Int) {
    override fun toString(): String {
        return "$name($sizeInBytes)"
    }
}