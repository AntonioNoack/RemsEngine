package me.anno.gpu.buffer

/**
 * Attributes together create a struct.
 * Once bound, every attribute has a fixed alignment (offset & stride).
 * */
abstract class AttributeLayout(
    val names: List<String>,
    val types: List<AttributeType>,
    val numComponents: ByteArray,
    val offsets: IntArray,
) {

    val size get() = names.size
    fun name(i: Int): String = names[i]
    fun type(i: Int): AttributeType = types[i]
    fun components(i: Int): Int = numComponents[i].toInt()
    fun offset(i: Int): Int = offsets[i]
    fun alignment(i: Int) = types[i].alignment(components(i))

    abstract fun stride(i: Int): Int
    abstract fun totalSize(numElements: Int): Int

    fun indexOf(name: String): Int = names.indexOf(name)

    override fun toString(): String {
        return "AttrLayout($names,$types,${numComponents.toList()}@${offsets.toList()})"
    }
}

