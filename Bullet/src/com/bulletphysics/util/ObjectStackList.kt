package com.bulletphysics.util

/**
 * Stack-based object pool for arbitrary objects, returning not supported.
 *
 * Example code:
 *
 * <pre>
 * StackList&lt;Vector3d&gt; vectors;
 * ...
 *
 * vectors.push();
 * try {
 * Vector3d vec = vectors.get();
 * ...
 * return vectors.returning(vec);
 * }
 * finally {
 * vectors.pop();
 * }
</pre> *
 *
 * @author jezek2
 */
class ObjectStackList<T: Any>(private val cls: Class<T>) {

    private val list = ArrayList<T>()

    private val stack = IntArray(512)
    private var stackCount = 0

    private var pos = 0

    /**
     * Pushes the stack.
     */
    fun push() {
        stack[stackCount++] = pos
    }

    /**
     * Pops the stack.
     */
    fun pop() {
        pos = stack[--stackCount]
    }

    /**
     * Returns instance from stack pool, or create one if not present. The returned
     * instance will be automatically reused when [.pop] is called.
     *
     * @return instance
     */
    fun get(): T {
        if (pos == list.size) {
            expand()
        }
        return list[pos++]
    }


    private fun expand() {
        list.add(create())
    }

    /**
     * Creates a new instance of type.
     *
     * @return instance
     */
    fun create(): T {
        return cls.newInstance()
    }
}
