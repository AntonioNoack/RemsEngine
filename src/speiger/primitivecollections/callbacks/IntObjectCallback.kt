package speiger.primitivecollections.callbacks

fun interface IntObjectCallback<V> {
    fun callback(key: Int, value: V)
}