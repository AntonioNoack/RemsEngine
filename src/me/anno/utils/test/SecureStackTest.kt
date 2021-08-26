package me.anno.utils.test

import me.anno.utils.LOGGER
import me.anno.utils.structures.SecureStack

val stack = object : SecureStack<Int>(0) {
    override fun onChangeValue(newValue: Int, oldValue: Int) {
        LOGGER.info("$oldValue -> $newValue")
    }
}

fun main() {

    try {
        stack.use(1) {
            stack.use(2) {
                stack.use(3) {
                    LOGGER.info("top")
                }
            }
        }

        LOGGER.info("level zero")

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

    LOGGER.info(stack)
    testFun(17) {
        LOGGER.info(stack)
    }


}

inline fun testFun(value: Int, consumer: () -> Unit) {
    stack.use(value, consumer)
}