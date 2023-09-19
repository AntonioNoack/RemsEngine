package me.anno.tests.structures

import me.anno.utils.structures.stacks.SecureStack
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class SecureStackTest {

    @Test
    fun test() {

        val transitions = intArrayOf(
            // from, to,
            0, 1,
            1, 15,
            15, 3,
            3, 15,
            15, 1,
            1, 0,
            0, 1,
            1, 2,
            2, 3,
            3, 2,
            2, 1,
            1, 0
        )

        var transitionIndex = 0
        val stack = object : SecureStack<Int>(0) {
            override fun onChangeValue(newValue: Int, oldValue: Int) {
                assertEquals(oldValue, transitions[transitionIndex++])
                assertEquals(newValue, transitions[transitionIndex++])
            }
        }

        stack.use(1) {
            assertEquals(stack.currentValue, 1)
            stack.use(15) {
                assertEquals(stack.currentValue, 15)
                stack.use(3) {
                    assertEquals(stack.currentValue, 3)
                }
                assertEquals(stack.currentValue, 15)
            }
            assertEquals(stack.currentValue, 1)
        }

        stack.use(1) {
            assertThrows<Exception> {
                stack.use(2) {
                    stack.use(3) {
                        throw Exception()
                    }
                }
            }
            assertEquals(stack.currentValue, 1)
        }
        assertEquals(stack.currentValue, 0)
        assertEquals(transitions.size, transitionIndex)
    }
}