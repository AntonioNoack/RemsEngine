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

    fun listToString(type: Char, list: List<Any>): String {
        val lengthLimit = 150
        val builder = StringBuilder(50)
        builder.append('[')
        builder.append(type)
        builder.append(':')
        builder.append(list.size.toString())
        var length = builder.length
        for (element in list) {
            val valueStr = element.toString()
            length += valueStr.length + 2
            if (length < lengthLimit) {
                builder.append(", ")
                builder.append(valueStr)
            } else break
        }
        builder.append(']')
        return builder.toString()
    }

    fun toString(it: Any): String {
        return when (it) {
            is IntArray -> listToString('i', it.toList())
            is LongArray -> listToString('l', it.toList())
            is FloatArray -> listToString('f', it.toList())
            is DoubleArray -> listToString('d', it.toList())
            else -> it.toString()
        }
    }

    fun format(it: Any) = "${toString(it)}:${it.javaClass.simpleName}"

    fun toString(depth: Int): String = "${Tabs.spaces(depth * 2)}$nameOrType: " +
            properties.joinToString(", ", "[", "]") { format(it) } + "\n" +
            children.joinToString("") { it.toString(depth + 1) }

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
        // "correct"
        // where do I know from, that it's correct?
        // the position comes last -> column-major format
        // and this would suggest, that this was incorrect...
        for (i in 0 until 16) {
            m.set(i / 4, i and 3, da[i].toFloat())
            // m.set(i and 3, i / 4, da[i].toFloat())
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