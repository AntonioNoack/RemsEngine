package com.bulletphysics

import cz.advel.stack.Stack.Companion.borrowVec3d
import cz.advel.stack.Stack.Companion.newVec3d
import cz.advel.stack.Stack.Companion.subVec3d

object StackTest {
    @JvmStatic
    fun main(args: Array<String>) {
        newVec3d()
        newVec3d(1.0)
        newVec3d(1.0, 2.0, 3.0)
        subVec3d(1)
        borrowVec3d()
        sub()
    }

    fun sub() {
        subVec3d(1)
    }
}
