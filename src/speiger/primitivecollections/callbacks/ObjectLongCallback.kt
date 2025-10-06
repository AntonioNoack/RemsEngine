package speiger.primitivecollections.callbacks

fun interface ObjectLongCallback<K> {
    fun call(key: K, value: Long)
}