package org.joml

import java.io.ObjectInput
import java.io.ObjectOutput

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

    /*
     * Contract between Matrix4f and Matrix4fStack:
     *
     * - Matrix4f.equals(Matrix4fStack) is true iff all the 16 matrix elements are equal
     * - Matrix4fStack.equals(Matrix4f) is true iff all the 16 matrix elements are equal
     * - Matrix4fStack.equals(Matrix4fStack) is true iff all 16 matrix elements are equal AND the matrix arrays as well as the stack pointer are equal
     * - everything else is inequal
     *
     * (non-Javadoc)
     * @see org.joml.Matrix4f#equals(java.lang.Object)
     */
    override fun equals(obj: Any?): Boolean {
        if (this === obj) return true
        if (!super.equals(obj)) return false
        if (obj is Matrix4fArrayList) {
            val other = obj
            if (currentIndex != other.currentIndex) return false
            for (i in 0 until currentIndex) {
                if (matrices[i] != other.matrices[i]) return false
            }
        }
        return true
    }

    override fun writeExternal(out: ObjectOutput) {
        super.writeExternal(out)
        out.writeInt(currentIndex)
        for (i in 0 until currentIndex) {
            out.writeObject(matrices[i])
        }
    }

    override fun readExternal(`in`: ObjectInput) {
        super.readExternal(`in`)
        currentIndex = `in`.readInt()
        matrices = ArrayList(currentIndex)
        for (i in 0 until currentIndex) {
            val m = Matrix4f()
            m.readExternal(`in`)
            matrices[i] = m
        }
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

    companion object {
        private const val serialVersionUID = 1L
    }
}