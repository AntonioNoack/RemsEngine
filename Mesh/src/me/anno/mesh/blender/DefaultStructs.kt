package me.anno.mesh.blender

class DefaultStructs(private val pointerSize: Int) {

    private fun create(type: DNAType) = DNAStruct(
        -1, type, arrayOf(
            DNAField(0, "i", type) // must be that for BVector1i
        ), pointerSize
    )

    private val intStruct = create(DNAType("int", 4))

    val byName = mapOf("int" to intStruct)
}