package cz.advel.stack

import java.nio.BufferUnderflowException
import java.util.function.Supplier

/**
 * this class is now fully thread safe :)
 * it is used to quickly borrow instances from specific classes used in bullet (javax.vecmath)
 */
class GenericStack<T>(private val generator: () -> T, private val name: String?) {

    private inner class GenericStackInstance {
        @Suppress("UNCHECKED_CAST")
        private var instances = arrayOfNulls<Any>(32) as Array<T>

        var position = 0

        init {
            val instances = instances
            for (i in instances.indices) {
                instances[i] = generator()
            }
        }

        // I either didn't find the library, or it was too large for my liking:
        // I rewrote the main functionalities
        fun reset2(printSlack: Boolean) {
            if (printSlack) {
                println(
                    "[BulletStack]: Slack: " +
                            position + " " + name
                )
            }
            position = 0
        }

        fun reset2(newPosition: Int) {
            position = newPosition
        }

        fun newInstance(): T {
            var vts = instances
            if (position >= vts.size) {
                val newSize = vts.size * 2
                checkLeaking(newSize)
                @Suppress("UNCHECKED_CAST")
                val values = arrayOfNulls<Any>(newSize) as Array<T>
                System.arraycopy(vts, 0, values, 0, vts.size)
                for (i in vts.size until newSize) {
                    values[i] = generator()
                }
                vts = values
                instances = vts
            }
            return vts[position++]
        }
    }

    private val instances =
        ThreadLocal.withInitial<GenericStackInstance>(Supplier { GenericStackInstance() })

    fun reset(printSlack: Boolean) {
        instances.get().reset2(printSlack)
    }

    fun reset(newPosition: Int) {
        instances.get().reset2(newPosition)
    }

    val position: Int
        get() = instances.get().position

    fun release(delta: Int) {
        val stack = instances.get()
        stack.position -= delta
        checkUnderflow(stack.position)
    }

    private fun checkLeaking(newSize: Int) {
        if (newSize > Stack.limit) throw OutOfMemoryError("Reached stack limit " + Stack.limit + ", probably leaking")
    }

    init {
        synchronized(STACKS) {
            STACKS.add(this)
        }
    }

    fun create(): T {
        val stack = instances.get()
        return stack.newInstance()
    }

    companion object {
        @JvmField
        val STACKS: ArrayList<GenericStack<*>> = ArrayList<GenericStack<*>>()

        private fun checkUnderflow(position: Int) {
            if (position < 0) throw BufferUnderflowException()
        }
    }
}
