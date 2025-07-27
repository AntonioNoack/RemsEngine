package speiger.primitivecollections.callbacks

fun interface LongLongPredicate {
    fun test(key: Long, value: Long): Boolean
}