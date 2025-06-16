package me.anno.gpu.buffer

/**
 * Attributes packed together create a struct.
 * Once bound, every attribute has a fixed alignment (offset & stride).
 * */
class AttributeLayout(
    val names: List<String>,
    val types: List<AttributeType>,
    val numComponents: ByteArray,
    val offsets: IntArray,
    val stride: Int
) {

    val size get() = names.size
    fun name(i: Int): String = names[i]
    fun type(i: Int): AttributeType = types[i]
    fun offset(i: Int): Int = offsets[i]
    fun components(i: Int): Int = numComponents[i].toInt()

    fun equals(i: Int, attribute: Attribute): Boolean {
        return names[i] == attribute.name &&
                types[i] == attribute.type &&
                numComponents[i].toInt() == attribute.numComponents
    }

    fun indexOf(name: String): Int = names.indexOf(name)

    override fun toString(): String {
        return "BoundAttributes($names,$types,${numComponents.toList()}@${offsets.toList()},$stride)"
    }

    operator fun plus(additional: List<Attribute>): AttributeLayout {
        val stride = stride + additional.sumOf { it.byteSize }
        val types = types + additional.map { it.type }
        val names = names + additional.map { it.name }
        val components = numComponents + ByteArray(additional.size) { additional[it].numComponents.toByte() }
        val offsets = calculateOffsets(this, additional)
        return AttributeLayout(names, types, components, offsets, stride)
    }

    companion object {

        val EMPTY = bind(emptyList())

        fun bind(attribute: Attribute): AttributeLayout {
            return bind(listOf(attribute))
        }

        fun bind(vararg attribute: Attribute): AttributeLayout {
            return bind(attribute.toList())
        }

        fun bind(attributes: List<Attribute>): AttributeLayout {
            val stride = attributes.sumOf { it.byteSize }
            val types = attributes.map { it.type }
            val names = attributes.map { it.name }
            val components = ByteArray(attributes.size) { attributes[it].numComponents.toByte() }
            val offsets = calculateOffsets(attributes)
            return AttributeLayout(names, types, components, offsets, stride)
        }

        private fun calculateOffsets(attributes: List<Attribute>): IntArray {
            return calculateOffsets(null, attributes)
        }

        private fun calculateOffsets(base: AttributeLayout?, attributes: List<Attribute>): IntArray {
            val baseSize = base?.size ?: 0
            val offsets = IntArray(baseSize + attributes.size)
            var offset = base?.stride ?: 0
            for (i in attributes.indices) {
                val src = attributes[i]
                offsets[baseSize + i] = offset
                offset += src.byteSize
            }
            return offsets
        }
    }
}

