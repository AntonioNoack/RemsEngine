package me.anno.utils.structures.arrays

import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f

object FloatArrayListUtils {

    fun FloatArrayList.addUnsafe(x: Float, y: Float) {
        val values = values
        var size = size
        values[size++] = x
        values[size++] = y
        this.size = size
    }

    fun FloatArrayList.addUnsafe(x: Float, y: Float, z: Float) {
        val values = values
        var size = size
        values[size++] = x
        values[size++] = y
        values[size++] = z
        this.size = size
    }

    fun FloatArrayList.addUnsafe(x: Float, y: Float, z: Float, w: Float) {
        val values = values
        var size = size
        values[size++] = x
        values[size++] = y
        values[size++] = z
        values[size++] = w
        this.size = size
    }

    fun FloatArrayList.addUnsafe(v: Vector3f) {
        addUnsafe(v.x, v.y, v.z)
    }

    fun FloatArrayList.add(v: Vector2f) {
        add(v.x, v.y)
    }

    fun FloatArrayList.add(v: Vector3f) {
        add(v.x, v.y, v.z)
    }

    fun FloatArrayList.add(v: Vector3d) {
        add(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
    }

    fun FloatArrayList.add(v: Vector4f) {
        add(v.x, v.y, v.z, v.w)
    }

    fun FloatArrayList.add(v: Quaternionf) {
        add(v.x, v.y, v.z, v.w)
    }

    fun FloatArrayList.add(v: Quaterniond) {
        add(v.x.toFloat(), v.y.toFloat(), v.z.toFloat(), v.w.toFloat())
    }

    fun FloatArrayList.add(x: Float, y: Float) {
        ensureExtra(2)
        val array = values
        var size = size
        array[size++] = x
        array[size++] = y
        this.size = size
    }

    fun FloatArrayList.add(x: Float, y: Float, z: Float) {
        ensureExtra(3)
        val array = values
        var size = size
        array[size++] = x
        array[size++] = y
        array[size++] = z
        this.size = size
    }

    fun FloatArrayList.add(x: Float, y: Float, z: Float, w: Float) {
        ensureExtra(4)
        val array = values
        var size = size
        array[size++] = x
        array[size++] = y
        array[size++] = z
        array[size++] = w
        this.size = size
    }

    operator fun FloatArrayList.plusAssign(v: Vector2f) {
        ensureExtra(2)
        val array = values
        var size = size
        array[size++] = v.x
        array[size++] = v.y
        this.size = size
    }

    operator fun FloatArrayList.plusAssign(v: Vector3f) {
        add(v)
    }
}