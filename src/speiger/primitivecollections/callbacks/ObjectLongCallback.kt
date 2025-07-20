package speiger.primitivecollections.callbacks

fun interface ObjectLongCallback<K> {
    fun callback(key: K, value: Long)
}