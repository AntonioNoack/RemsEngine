package speiger.primitivecollections.callbacks

fun interface LongDoubleCallback {
    fun call(key: Long, value: Double)
}