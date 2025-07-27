package speiger.primitivecollections.callbacks

fun interface LongPredicate {
    fun test(key: Long): Boolean
}