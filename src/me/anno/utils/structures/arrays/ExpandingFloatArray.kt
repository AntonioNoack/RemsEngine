package me.anno.utils.structures.arrays

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.max

class ExpandingFloatArray(
    private var initCapacity: Int
) : Saveable() {

    var size = 0

    private var array: FloatArray? = null

    fun clear() {
        size = 0
    }

    fun ensure() {
        if (array == null) {
            array = FloatArray(initCapacity)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("initCapacity", initCapacity)
        writer.writeInt("size", size)
        val array = array
        if (array != null) {
            // clear the end, so we can save it with less space wasted
            for (i in size until array.size) {
                array[i] = 0f
            }
            writer.writeFloatArray("values", array)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "size" -> size = value
            "initCapacity" -> initCapacity
            else -> super.readInt(name, value)
        }
    }

    override fun readFloatArray(name: String, values: FloatArray) {
        when (name) {
            "values" -> array = values
            else -> super.readFloatArray(name, values)
        }
    }

    fun add(value: Float) {
        val array = array
        if (array == null || size + 1 >= array.size) {
            val newArray = FloatArray(if (array == null) initCapacity else max(array.size * 2, 16))
            if (array != null) System.arraycopy(array, 0, newArray, 0, size)
            this.array = newArray
            newArray[size++] = value
        } else {
            array[size++] = value
        }
    }

    fun addUnsafe(value: Float) {
        array!![size++] = value
    }

    operator fun set(index: Int, value: Float) {
        array!![index] = value
    }

    fun add(v: Vector3f) {
        add(v.x)
        add(v.y)
        add(v.z)
    }

    fun add(v: FloatArray, startIndex: Int, length: Int) {
        for (i in 0 until length) {
            add(v[startIndex + i])
        }
    }

    fun addUnsafe(v: FloatArray, startIndex: Int, length: Int) {
        for (i in 0 until length) {
            addUnsafe(v[startIndex + i])
        }
    }

    operator fun get(index: Int) = array!![index]
    operator fun plusAssign(value: Float) {
        add(value)
    }

    fun toFloatArray(): FloatArray {
        if (size == array?.size) return array!!
        val tmp = FloatArray(size)
        if (size > 0) System.arraycopy(array!!, 0, tmp, 0, size)
        return tmp
    }

    operator fun plusAssign(v: Vector2f) {
        plusAssign(v.x)
        plusAssign(v.y)
    }

    operator fun plusAssign(v: Vector3f) {
        plusAssign(v.x)
        plusAssign(v.y)
        plusAssign(v.z)
    }

}