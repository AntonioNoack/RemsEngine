package speiger.primitivecollections.callbacks

fun interface IntObjectPredicate<V> {
    fun test(key: Int, value: V): Boolean
}