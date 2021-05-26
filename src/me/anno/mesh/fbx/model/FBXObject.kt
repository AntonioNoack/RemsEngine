package me.anno.mesh.fbx.model

import me.anno.mesh.fbx.structure.FBXNode
import me.anno.utils.Tabs
import org.joml.Vector3f

open class FBXObject(node: FBXNode) {

    val ptr = node.getId()
    val name = node.getName()
    val subType = node.properties.getOrNull(2) as? String ?: "#"

    val children = ArrayList<FBXObject>()
    val parents = ArrayList<FBXObject>()

    val overrides = ArrayList<Pair<String, FBXObject>>()

    // not used
    // val version = (node.getProperty("Version") ?: node.getProperty("GeometryVersion") ?: node.getProperty("KeyVer")) as? Int

    fun applyProperties(data: FBXNode) {
        for (property in data.children.filter { it.nameOrType == "Properties70" }) {
            applyProperty(property)
        }
    }

    private fun applyProperty(property: FBXNode) {
        for (child in property.children) {
            applyProperty(child.properties)
        }
    }

    private fun applyProperty(p: Array<Any>) {
        val name = p[0] as String
        val type = p[1] as String
        val offset = 4
        val value = when (type) {
            "Color", "ColorRGB", "Vector", "Vector3D", "Lcl Rotation", "Lcl Translation", "Lcl Scaling" -> {
                Vector3f(getFloat(p[offset]), getFloat(p[offset + 1]), getFloat(p[offset + 2]))
            }
            "Number", "double", "FieldOfView", "FieldOfViewX", "FieldOfViewY" -> getFloat(p[offset])
            "KTime" -> getLong(p[offset])
            "KString", "KRefUrl", "charptr" -> p[offset].toString()
            "int", "Integer", "enum", "Action" -> getBool(p[offset])
            "bool", "Bool" -> when (val value = p[offset]) {
                is Int -> value != 0
                is Long -> value != 0
                is Boolean -> value
                else -> throw RuntimeException("$value, ${value.javaClass.simpleName} is not a boolean")
            }
            "Visibility" -> getFloat(p[offset])
            "object" -> null // doesn't have a value
            else -> throw RuntimeException(
                "Unknown type $type for $name, example value: ${
                    p.filterIndexed { index, _ -> index >= offset }.joinToString()
                }"
            )
        }
        if (value != null) {
            onReadProperty70(name, value)
        }
    }

    open fun onReadProperty70(name: String, value: Any) {
        when (name) {
            "d|DefaultAttributeIndex" -> Unit // ?? what does this do?? information about local position???
            "d|Visibility" -> Unit // not interesting
            "d|liw" -> Unit // probably not interesting, look influence weight
            else -> throw RuntimeException("Unknown property $name: $value for ${javaClass.simpleName}")
        }
    }

    open fun toString(depth0: Int, depth: Int, filter: (parent: FBXObject, child: FBXObject) -> Boolean): String {
        val tabs = Tabs.spaces(depth * 2 + 1)
        return Tabs.spaces(depth0 * 2) +
                "$depth ${javaClass.simpleName.substring(3)}:$name:$subType\n" +
                overrides.filter { filter(this, it.second) }
                    .joinToString("") { (key, value) -> "$tabs $key: ${value.toString(0, depth + 1, filter)}" } +
                children.filter { filter(this, it) }.joinToString("") { it.toString(depth + 1, depth + 1, filter) }
    }

    override fun toString() = toString(0, 0) { _, _ -> true }

    companion object {

        fun getFloat(any: Any?): Float {
            return when (any) {
                is Float -> any
                is Double -> any.toFloat()
                is Int -> any.toFloat()
                is Long -> any.toFloat()
                else -> any.toString().toFloat()
            }
        }

        fun getDouble(any: Any?): Double {
            return when (any) {
                is Float -> any.toDouble()
                is Double -> any
                is Int -> any.toDouble()
                is Long -> any.toDouble()
                else -> any.toString().toDouble()
            }
        }

        fun getInt(any: Any?): Int {
            return when (any) {
                is Float -> any.toInt()
                is Double -> any.toInt()
                is Int -> any
                is Long -> any.toInt()
                else -> any.toString().toInt()
            }
        }

        fun getLong(any: Any?): Long {
            return when (any) {
                is Float -> any.toLong()
                is Double -> any.toLong()
                is Int -> any.toLong()
                is Long -> any
                else -> any.toString().toLong()
            }
        }

        fun getBool(any: Any?): Boolean {
            return when (any) {
                is Float -> any != 0f
                is Double -> any != 0.0 && any.isFinite()
                is Int -> any != 0
                is Long -> any != 0
                is Boolean -> any
                else -> any.toString().toBoolean()
            }
        }

    }

}