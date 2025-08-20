package speiger.primitivecollections.callbacks

fun interface IntPredicate {
    fun test(key: Int): Boolean
}