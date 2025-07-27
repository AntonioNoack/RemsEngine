package speiger.primitivecollections.callbacks

fun interface ObjectIntPredicate<K> {
    fun test(key: K, value: Int): Boolean
}