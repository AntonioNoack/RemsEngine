package speiger.primitivecollections.callbacks

fun interface IntObjectCallback<V> {
    fun call(key: Int, value: V)
}