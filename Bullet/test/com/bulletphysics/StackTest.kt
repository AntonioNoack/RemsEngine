package com.bulletphysics

import cz.advel.stack.Stack.Companion.borrowVec
import cz.advel.stack.Stack.Companion.newVec
import cz.advel.stack.Stack.Companion.subVec

object StackTest {
    @JvmStatic
    fun main(args: Array<String>) {
        newVec()
        newVec(1.0)
        newVec(1.0, 2.0, 3.0)
        subVec(1)
        borrowVec()
        sub()
    }

    fun sub() {
        subVec(1)
    }
}
