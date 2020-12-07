package me.anno.mesh.fbx.model

import me.anno.mesh.fbx.structure.FBXNode
import me.anno.utils.Tabs
import org.joml.Vector3f

open class FBXObject(node: FBXNode) {

    val ptr = node.getId()
    val name = node.getName()
    val subType = node.properties.getOrNull(2) as? String ?: "#"

    val children = ArrayList<FBXObject>()
    val overrides = HashMap<String, FBXObject>()

    val version =
        (node.getProperty("Version") ?: node.getProperty("GeometryVersion") ?: node.getProperty("KeyVer")) as? Int

    fun applyProperties(data: FBXNode) {
        data["Properties70"].forEach { ps ->
            ps.children.map { it.properties }.forEach { p ->
                val name = p[0] as String
                val type = p[1] as String
                val offset = 4
                val value = when (type) {
                    "Color", "ColorRGB", "Vector", "Vector3D", "Lcl Rotation", "Lcl Translation", "Lcl Scaling" -> {
                        Vector3f(
                            (p[offset] as Double).toFloat(),
                            (p[offset + 1] as Double).toFloat(),
                            (p[offset + 2] as Double).toFloat()
                        )
                    }
                    "Number", "double", "FieldOfView", "FieldOfViewX", "FieldOfViewY"
                    -> (p[offset] as Double).toFloat()
                    "KTime" -> p[offset] as Long
                    "KString", "KRefUrl", "charptr" -> p[offset] as String
                    "int", "Integer", "enum", "Action" -> p[offset] as Int
                    "bool", "Bool" -> when (val value = p[offset]) {
                        is Int -> value != 0
                        is Boolean -> value
                        else -> throw RuntimeException("$value is not a boolean")
                    }
                    "Visibility" -> p[offset] as Double
                    "object" -> null // doesn't have a value
                    else -> throw RuntimeException("Unknown type $type for $name, example value: ${p.filterIndexed { index, _ -> index >= offset }.joinToString()}")
                }
                if (value != null) {
                    onReadProperty70(name, value)
                }
            }
        }
    }

    open fun onReadProperty70(name: String, value: Any) {
        when(name){
            "d|DefaultAttributeIndex" -> Unit // ?? what does this do?? information about local position???
            "d|Visibility" -> Unit // not interesting
            "d|liw" -> Unit // probably not interesting, look influence weight
            else -> throw RuntimeException("Unknown property $name: $value for ${javaClass.simpleName}")
        }
    }

    open fun toString(depth: Int): String = Tabs.spaces(depth * 2) +
            "$depth ${javaClass.simpleName.substring(3)}:$name:$subType\n" +
            overrides.entries.joinToString("") { Tabs.spaces(depth * 2 + 1) + "${it.key}: ${it.value}\n" } +
            children.joinToString("") { it.toString(depth + 1) }

    override fun toString() = toString(0)

}