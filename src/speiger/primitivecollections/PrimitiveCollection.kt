package speiger.primitivecollections

interface PrimitiveCollection {

    val size: Int
    val maxFill: Int

    fun isEmpty() = size == 0
    fun isNotEmpty() = !isEmpty()

    /** Removes all entries; keeps the current capacity */
    fun clear()

    /**
     * Removes all entries; reduces the current capacity to size, if capacity > size.
     * */
    fun clearAndTrim(size: Int)
}