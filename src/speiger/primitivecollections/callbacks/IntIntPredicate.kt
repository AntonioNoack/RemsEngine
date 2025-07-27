package speiger.primitivecollections.callbacks

fun interface IntIntPredicate {
    fun test(key: Int, value: Int): Boolean
}