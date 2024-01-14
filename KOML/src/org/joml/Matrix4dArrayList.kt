package org.joml

/**
 * A stack of many [Matrix4d] instances. This resembles the matrix stack known from legacy OpenGL.
 *
 *
 * This [Matrix4dArrayList] class inherits from [Matrix4d], so the current/top matrix is always the [Matrix4dArrayList]/[Matrix4d] itself. This
 * affects all operations in [Matrix4d] that take another [Matrix4d] as parameter. If a [Matrix4dArrayList] is used as argument to those methods,
 * the effective argument will always be the *current* matrix of the matrix stack.
 *
 * @author Kai Burjack, modified by Antonio Noack to be infinite, yet not allocation free
 * our users shouldn't have to worry about tree depth, if their machine is strong enough to handle it
 */
class Matrix4dArrayList : Matrix4d() {

    /**
     * The matrix stack
     */
    private val matrices = ArrayList<Matrix4d>()

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
    fun clear(): Matrix4dArrayList {
        currentIndex = 0
        identity()
        return this
    }

    /**
     * Increment the stack pointer by one and set the values of the new current matrix to the one directly below it.
     *
     * @return this
     */
    fun pushMatrix(): Matrix4dArrayList {
        if (currentIndex == matrices.size) {
            matrices.add(Matrix4d())
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
    fun popMatrix(): Matrix4dArrayList {
        check(currentIndex != 0) {
            "already at the buttom of the stack" //$NON-NLS-1$
        }
        set(matrices[--currentIndex])
        return this
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = super.hashCode()
        result = prime * result + currentIndex
        for (i in 0 until currentIndex) {
            result = prime * result + matrices[i].hashCode()
        }
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (!super.equals(obj)) return false
        if (obj is Matrix4dArrayList) {
            if (currentIndex != obj.currentIndex) return false
            for (i in 0 until currentIndex) {
                if (matrices[i] != obj.matrices[i]) return false
            }
        }
        return true
    }

}