package com.bulletphysics.util

import java.io.Externalizable
import java.io.IOException
import java.io.ObjectInput
import java.io.ObjectOutput
import java.util.*

/**
 * @author jezek2
 */
class ObjectArrayList<T>(initialCapacity: Int = 16) :
    AbstractList<T>(), RandomAccess, Externalizable {

    private var array: Array<T>
    override var size: Int = 0
        private set

    init {
        array = arrayOfNulls<Any>(initialCapacity) as Array<T>
    }

    override fun add(value: T): Boolean {
        if (size == array.size) {
            expand()
        }

        array[size++] = value
        return true
    }

    override fun add(index: Int, value: T) {
        if (size == array.size) {
            expand()
        }

        val num = size - index
        if (num > 0) {
            System.arraycopy(array, index, array, index + 1, num)
        }

        array[index] = value
        size++
    }

    override fun removeAt(index: Int): T {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException()
        val prev = array[index]
        System.arraycopy(array, index + 1, array, index, size - index - 1)
        array[size - 1] = null as T
        size--
        return prev as T
    }

    private fun expand() {
        val newArray = arrayOfNulls<Any>(array.size shl 1) as Array<T>
        System.arraycopy(array, 0, newArray, 0, array.size)
        array = newArray
    }

    fun removeQuick(index: Int) {
        System.arraycopy(array, index + 1, array, index, size - index - 1)
        array[size - 1] = null as T
        size--
    }

    fun swapRemove(instance: T) {
        val index = indexOf(instance)
        if (index >= 0) {
            swapRemove(index)
        }
    }

    fun swapRemove(index: Int) {
        size--
        if (index < size) array[index] = array[size]
        array[size] = null as T
    }

    override fun get(index: Int): T {
        if (index >= size) throw IndexOutOfBoundsException()
        return array[index]
    }

    fun getQuick(index: Int): T {
        return array[index]
    }

    override fun set(index: Int, value: T): T {
        if (index >= size) throw IndexOutOfBoundsException()
        val old = array[index]
        array[index] = value
        return old
    }

    fun setQuick(index: Int, value: T) {
        array[index] = value
    }

    fun capacity(): Int {
        return array.size
    }

    override fun clear() {
        size = 0
    }

    override fun indexOf(o: T): Int {
        val _array = array
        var i = 0
        val _size = size
        while (i < _size) {
            if (o == _array[i]) {
                return i
            }
            i++
        }
        return -1
    }

    @Throws(IOException::class)
    override fun writeExternal(output: ObjectOutput) {
        output.writeInt(size)
        for (i in 0 until size) {
            output.writeObject(array[i])
        }
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    override fun readExternal(input: ObjectInput) {
        size = input.readInt()
        var cap = 16
        while (cap < size) cap = cap shl 1
        array = arrayOfNulls<Any>(cap) as Array<T>
        for (i in 0 until size) {
            array[i] = input.readObject() as T
        }
    }
}
