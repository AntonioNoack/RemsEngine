package speiger.primitivecollections.callbacks

fun interface LongObjectCallback<V> {
    fun call(key: Long, value: V)
}