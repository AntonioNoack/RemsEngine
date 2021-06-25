package me.anno.utils.test

import me.anno.utils.structures.SecureStack

val stack = object : SecureStack<Int>(0) {
    override fun onChangeValue(newValue: Int, oldValue: Int) {
        println("$oldValue -> $newValue")
    }
}

fun main() {

    try {
        stack.use(1) {
            stack.use(2) {
                stack.use(3) {
                    println("top")
                }
            }
        }

        println("level zero")

        stack.use(1) {
            stack.use(2) {
                stack.use(3) {
                    throw Exception()
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    println(stack)
    testFun(17){
        println(stack)
    }


}

inline fun testFun(value: Int, consumer: () -> Unit) {
    stack.use(value, consumer)
}