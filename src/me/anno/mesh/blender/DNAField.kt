package me.anno.mesh.blender

/**
 * decorated name e.g. is **mat, x[1][3] or sth like that
 * */
class DNAField(val index: Int, val decoratedName: String, val type: DNAType) {

    var offset = 0
    var arraySizeOr1 = 1
    var isPointer = decoratedName.startsWith("*")

    override fun toString(): String {
        return "${type.name}(${type.size})@$offset"
    }

}