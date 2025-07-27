package speiger.primitivecollections.callbacks

fun interface ObjectLongPredicate<K> {
    fun test(key: K, value: Long): Boolean
}