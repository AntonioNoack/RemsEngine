package me.anno.utils.test.structures

import me.anno.utils.structures.stacks.SecureStack
import org.apache.logging.log4j.LogManager


fun main() {

    val logger = LogManager.getLogger("SecureStackTest")

    val stack = object : SecureStack<Int>(0) {
        override fun onChangeValue(newValue: Int, oldValue: Int) {
            logger.info("$oldValue -> $newValue")
        }
    }

    fun testFun(value: Int, consumer: () -> Unit) {
        stack.use(value, consumer)
    }

    try {
        stack.use(1) {
            stack.use(2) {
                stack.use(3) {
                    logger.info("top")
                }
            }
        }

        logger.info("level zero")

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

    logger.info(stack)
    testFun(17) {
        logger.info(stack)
    }


}