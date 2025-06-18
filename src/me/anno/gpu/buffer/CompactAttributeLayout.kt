package me.anno.gpu.buffer

/**
 * Attributes packed (std140) together create a struct.
 * Once bound, every attribute has a fixed alignment (offset & stride).
 * */
class CompactAttributeLayout(
    names: List<String>,
    types: List<AttributeType>,
    numComponents: ByteArray,
    offsets: IntArray,
    val stride: Int
) : AttributeLayout(names, types, numComponents, offsets) {

    override fun stride(i: Int): Int = stride
    override fun totalSize(numElements: Int): Int {
        return Math.multiplyExact(numElements, stride)
    }

    operator fun plus(additional: List<Attribute>): CompactAttributeLayout {
        val stride = stride(0) + additional.sumOf { it.byteSize }
        val types = types + additional.map { it.type }
        val names = names + additional.map { it.name }
        val components = numComponents + ByteArray(additional.size) { additional[it].numComponents.toByte() }
        val offsets = calculateOffsets(this, additional)
        return CompactAttributeLayout(names, types, components, offsets, stride)
    }

    companion object {

        val EMPTY = bind(emptyList())

        /**
         * Defines a compact (std140) layout without any consideration for alignments.
         * */
        fun bind(attribute: Attribute): CompactAttributeLayout {
            return bind(listOf(attribute))
        }

        /**
         * Defines a compact (std140) layout without any consideration for alignments.
         * */
        fun bind(vararg attribute: Attribute): CompactAttributeLayout {
            return bind(attribute.toList())
        }

        /**
         * Defines a compact (std140) layout without any consideration for alignments.
         * */
        fun bind(attributes: List<Attribute>): CompactAttributeLayout {
            val stride = attributes.sumOf { it.byteSize }
            val types = attributes.map { it.type }
            val names = attributes.map { it.name }
            val numComponents = ByteArray(attributes.size) { attributes[it].numComponents.toByte() }
            val offsets = calculateOffsets(attributes)
            return CompactAttributeLayout(names, types, numComponents, offsets, stride)
        }

        /**
         * Defines a compact (std140) layout without any consideration for alignments.
         * */
        private fun calculateOffsets(attributes: List<Attribute>): IntArray {
            return calculateOffsets(null, attributes)
        }

        /**
         * Defines a compact (std140) layout without any consideration for alignments.
         * */
        private fun calculateOffsets(base: CompactAttributeLayout?, attributes: List<Attribute>): IntArray {
            val baseSize = base?.size ?: 0
            val offsets = IntArray(baseSize + attributes.size)
            var offset = if (base != null && base.size > 0) base.stride(0) else 0
            for (i in attributes.indices) {
                val src = attributes[i]
                offsets[baseSize + i] = offset
                offset += src.byteSize
            }
            return offsets
        }
    }
}

