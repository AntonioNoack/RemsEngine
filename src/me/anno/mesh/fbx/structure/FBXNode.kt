package me.anno.mesh.fbx.structure

import me.anno.mesh.fbx.model.FBXObject.Companion.getDouble
import me.anno.mesh.fbx.model.FBXObject.Companion.getFloat
import me.anno.mesh.fbx.model.FBXObject.Companion.getInt
import me.anno.mesh.fbx.model.FBXObject.Companion.getLong
import me.anno.utils.Tabs
import org.joml.Matrix4f

// ObjectType defined the default values...

class FBXNode(val nameOrType: String, val properties: Array<Any>) : FBXNodeBase {

    override val children = ArrayList<FBXNode>()

    fun toString(depth: Int): String = "${Tabs.spaces(depth * 2)}$nameOrType: " +
            "${
                properties.joinToString(", ", "[", "]") {
                    "${
                        when (it) {
                            is IntArray -> "[i:${it.size}, " + it.filterIndexed { index, _ -> index < 50 }
                                .joinToString()
                            is LongArray -> "[l:${it.size}, " + it.filterIndexed { index, _ -> index < 50 }
                                .joinToString()
                            is FloatArray -> "[f:${it.size}, " + it.filterIndexed { index, _ -> index < 50 }
                                .joinToString()
                            is DoubleArray -> "[d:${it.size}, " + it.filterIndexed { index, _ -> index < 50 }
                                .joinToString()
                            else -> it.toString()
                        }
                    }:${it.javaClass.simpleName}"
                }
            }\n${
                children.joinToString("") {
                    it.toString(depth + 1)
                }
            }"

    override fun toString() = toString(0)

    fun getProperty(name: String) =
        getFirst(name)?.properties?.first()

    fun getIntArray(name: String): IntArray? {
        return when (val array = getProperty(name)) {
            is FloatArray -> IntArray(array.size) { array[it].toInt() }
            is DoubleArray -> IntArray(array.size) { array[it].toInt() }
            is IntArray -> array
            is LongArray -> IntArray(array.size) { array[it].toInt() }
            else -> {
                val firstMaybe = getFirst(name)?.properties
                val array2 = array as? Array<*> ?: firstMaybe ?: return null
                IntArray(array2.size) { getInt(array2[it]) }
            }
        }
    }

    fun getLongArray(name: String): LongArray? {
        return when (val array = getProperty(name)) {
            is FloatArray -> LongArray(array.size) { array[it].toLong() }
            is DoubleArray -> LongArray(array.size) { array[it].toLong() }
            is IntArray -> LongArray(array.size) { array[it].toLong() }
            is LongArray -> array
            else -> {
                val firstMaybe = getFirst(name)?.properties
                val array2 = array as? Array<*> ?: firstMaybe ?: return null
                LongArray(array2.size) { getLong(array2[it]) }
            }
        }
    }

    fun getFloatArray(name: String): FloatArray? {
        return when (val array = getProperty(name)) {
            is FloatArray -> array
            is DoubleArray -> FloatArray(array.size) { array[it].toFloat() }
            is IntArray -> FloatArray(array.size) { array[it].toFloat() }
            is LongArray -> FloatArray(array.size) { array[it].toFloat() }
            else -> {
                val firstMaybe = getFirst(name)?.properties
                val array2 = array as? Array<*> ?: firstMaybe ?: return null
                FloatArray(array2.size) { getFloat(array2[it]) }
            }
        }
    }

    fun getDoubleArray(name: String): DoubleArray? {
        return when (val array = getProperty(name)) {
            is FloatArray -> return DoubleArray(array.size) { array[it].toDouble() }
            is DoubleArray -> return array
            is IntArray -> return DoubleArray(array.size) { array[it].toDouble() }
            is LongArray -> return DoubleArray(array.size) { array[it].toDouble() }
            else -> {
                val firstMaybe = getFirst(name)?.properties
                val array2 = array as? Array<*> ?: firstMaybe ?: return null
                DoubleArray(array2.size) { getDouble(array2[it]) }
            }
        }
    }

    fun getBoolean(name: String): Boolean? {
        val value = getProperty(name) ?: return null
        return when (value) {
            0, false, "0", "false" -> false
            1, true, "1", "true" -> true
            else -> true
        }
    }

    fun getM4x4(name: String): Matrix4f? {
        val da = getDoubleArray(name) ?: return null
        if (da.size != 16) throw RuntimeException("Got mat4x4 of size ${da.size}, expected 16")
        val m = Matrix4f()
        // correct
        for (i in 0 until 16) {
            m.set(i / 4, i and 3, da[i].toFloat())
        }
        return m
    }

    fun getName() = (properties.getOrNull(1) as? String)?.split(0.toChar())?.get(0) ?: "#"
    fun getId() = when (val id = properties[0]) {
        is Int -> id.toLong()
        is Long -> id
        else -> throw RuntimeException("ID of unknown type $id, ${id.javaClass.simpleName}")
    }

}