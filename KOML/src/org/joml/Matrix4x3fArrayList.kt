package org.joml

/**
 * A stack of many [Matrix4x3f] instances. This resembles the matrix stack known from legacy OpenGL.
 *
 *
 * This [Matrix4x3fArrayList] class inherits from [Matrix4x3f], so the current/top matrix is always the [Matrix4x3fArrayList]/[Matrix4x3f] itself. This
 * affects all operations in [Matrix4x3f] that take another [Matrix4x3f] as parameter. If a [Matrix4x3fArrayList] is used as argument to those methods,
 * the effective argument will always be the *current* matrix of the matrix stack.
 *
 * @author Kai Burjack, modified by Antonio Noack to be infinite, yet not allocation free
 * our users shouldn't have to worry about tree depth, if their machine is strong enough to handle it
 */
class Matrix4x3fArrayList : Matrix4x3f() {
    /**
     * The matrix stack
     */
    private var matrices = ArrayList<Matrix4x3f>()

    /**
     * The index of the "current" matrix within [.matrices].
     */
    var currentIndex = 0
        private set

    val size: Int get() = matrices.size

    /**
     * Set the stack pointer to zero and set the current/bottom matrix to [identity][.identity].
     *
     * @return this
     */
    fun clear(): Matrix4x3fArrayList {
        currentIndex = 0
        identity()
        return this
    }

    /**
     * Increment the stack pointer by one and set the values of the new current matrix to the one directly below it.
     *
     * @return this
     */
    fun pushMatrix(): Matrix4x3fArrayList {
        if (currentIndex == matrices.size) {
            matrices.add(Matrix4x3f())
        }
        matrices[currentIndex++].set(this)
        return this
    }

    /**
     * Decrement the stack pointer by one.
     *
     *
     * This will effectively dispose of the current matrix.
     *
     * @return this
     */
    fun popMatrix(): Matrix4x3fArrayList {
        assert(currentIndex > 0) { "already at the bottom of the stack" }
        set(matrices[--currentIndex])
        return this
    }
}