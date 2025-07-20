package speiger.primitivecollections.callbacks

fun interface ObjectIntCallback<K> {
    fun callback(key: K, value: Int)
}