package speiger.primitivecollections.callbacks

fun interface LongObjectPredicate<V> {
    fun test(key: Long, value: V): Boolean
}