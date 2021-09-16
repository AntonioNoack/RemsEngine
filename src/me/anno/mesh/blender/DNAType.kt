package me.anno.mesh.blender

class DNAType(
    val name: String, val size: Int, val pointerSize: Int
) {
    override fun toString(): String {
        return "DNAType[name=$name, size=$size]"
    }
}