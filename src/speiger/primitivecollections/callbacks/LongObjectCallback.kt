package speiger.primitivecollections.callbacks

fun interface LongObjectCallback<V> {
    fun callback(key: Long, value: V)
}