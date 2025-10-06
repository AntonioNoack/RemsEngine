package speiger.primitivecollections.callbacks

fun interface ObjectIntCallback<K> {
    fun call(key: K, value: Int)
}