package speiger.primitivecollections.callbacks

fun interface LongIntPredicate {
    fun test(key: Long, value: Int): Boolean
}