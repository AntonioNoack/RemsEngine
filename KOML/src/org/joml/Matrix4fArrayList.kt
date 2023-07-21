package org.joml

/**
 * A stack of many [Matrix4f] instances. This resembles the matrix stack known from legacy OpenGL.
 *
 *
 * This [Matrix4fArrayList] class inherits from [Matrix4f], so the current/top matrix is always the [Matrix4fArrayList]/[Matrix4f] itself. This
 * affects all operations in [Matrix4f] that take another [Matrix4f] as parameter. If a [Matrix4fArrayList] is used as argument to those methods,
 * the effective argument will always be the *current* matrix of the matrix stack.
 *
 * @author Kai Burjack, modified by Antonio Noack to be infinite, yet not allocation free
 * users of RemsStudio shouldn't have to worry about tree depth, if their machine is strong enough to handle it
 */
class Matrix4fArrayList : Matrix4f() {

    /**
     * The matrix stack
     */
    private var matrices = ArrayList<Matrix4f>()

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
    fun clear(): Matrix4fArrayList {
        currentIndex = 0
        identity()
        return this
    }

    /**
     * Increment the stack pointer by one and set the values of the new current matrix to the one directly below it.
     *
     * @return this
     */
    fun pushMatrix(): Matrix4fArrayList {
        if (currentIndex == matrices.size) {
            matrices.add(Matrix4f())
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
    fun popMatrix(): Matrix4fArrayList {
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

    override fun equals(m: Any?): Boolean {
        if (this === m) return true
        if (!super.equals(m)) return false
        if (m is Matrix4fArrayList) {
            if (currentIndex != m.currentIndex) return false
            for (i in 0 until currentIndex) {
                if (matrices[i] != m.matrices[i]) return false
            }
        }
        return true
    }

    /**
     * + reduces errors, because neither push nor pop can be forgotten, even if errors happen
     * - allocated a lambda
     */
    fun <V> next(run: Function0<V>): V {
        pushMatrix()
        val result: V
        try {
            result = run.invoke()
            popMatrix()
        } catch (e: Throwable) {
            popMatrix()
            throw e
        }
        return result
    }

}