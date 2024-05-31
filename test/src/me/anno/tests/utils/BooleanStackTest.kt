package me.anno.tests.utils

import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.arrays.BooleanArrayList
import org.junit.jupiter.api.Test
import kotlin.random.Random

object BooleanStackTest {

    @Test
    fun test() {
        val random = Random(1234L)
        val sequence = (0 until 10).map { random.nextBoolean() }
        val stack = BooleanArrayList()
        for (i in sequence) {
            stack.push(i)
        }
        for (i in sequence.reversed()) {
            assertEquals(i, stack.pop())
        }
    }
}