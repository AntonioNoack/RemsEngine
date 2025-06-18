package me.anno.gpu.buffer

import me.anno.utils.assertions.assertEquals

/**
 * Attributes (std430) together, respecting individual alignments.
 * Fixed to a certain number of elements.
 * */
class StridedAttributeLayout(
    names: List<String>,
    types: List<AttributeType>,
    numComponents: ByteArray,
    offsets: IntArray,
    private val numElements: Int,
    private val totalSize: Int,
    private val strides: IntArray
) : AttributeLayout(names, types, numComponents, offsets) {
    override fun stride(i: Int): Int = strides[i]
    override fun totalSize(numElements: Int): Int {
        assertEquals(this.numElements, numElements)
        return totalSize
    }
}

