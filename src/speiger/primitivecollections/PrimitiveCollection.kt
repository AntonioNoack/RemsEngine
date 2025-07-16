package speiger.primitivecollections

interface PrimitiveCollection {

    val size: Int
    val maxFill: Int

    fun isEmpty() = size == 0
    fun isNotEmpty() = !isEmpty()

    fun clear()
}