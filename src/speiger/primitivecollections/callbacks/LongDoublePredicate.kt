package speiger.primitivecollections.callbacks

fun interface LongDoublePredicate {
    fun test(key: Long, value: Double): Boolean
}