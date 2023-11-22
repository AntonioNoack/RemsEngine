package me.anno.io.unity

import me.anno.io.yaml.YAMLNode
import me.anno.io.yaml.YAMLReader
import me.anno.utils.strings.StringHelper.indexOf2
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.min

fun YAMLNode.getColorAsVector4f(name: String): Vector4f? = this[name]?.getColorAsVector4f()
fun YAMLNode.getColorAsVector4f(): Vector4f? {
    val str = value ?: return null
    var r = 1f
    var g = 1f
    var b = 1f
    var a = 1f
    parseYAMLxJSON(str) { key, value ->
        val asFloat = value.toFloatOrNull()
        if (asFloat != null) {
            when (key) {
                "r", "x" -> r = asFloat
                "g", "y" -> g = asFloat
                "b", "z" -> b = asFloat
                "a", "w" -> a = asFloat
            }
        }
    }
    return Vector4f(r, g, b, a)
}

fun YAMLNode.getColorAsVector3f(name: String): Vector3f? = this[name]?.getColorAsVector3f()
fun YAMLNode.getColorAsVector3f(): Vector3f? {
    val str = value ?: return null
    var r = 1f
    var g = 1f
    var b = 1f
    parseYAMLxJSON(str) { key, value ->
        val asFloat = value.toFloatOrNull()
        if (asFloat != null) {
            when (key) {
                "r", "x" -> r = asFloat
                "g", "y" -> g = asFloat
                "b", "z" -> b = asFloat
            }
        }
    }
    return Vector3f(r, g, b)
}

fun YAMLNode.getVector3d(significance: Double): Vector3d? {
    val str = value ?: return null
    var x = 0.0
    var y = 0.0
    var z = 0.0
    parseYAMLxJSON(str) { key, value ->
        val parsed = value.toDoubleOrNull()
        if (parsed != null) {
            when (key) {
                "r", "x" -> x = parsed
                "g", "y" -> y = parsed
                "b", "z" -> z = parsed
            }
        }
    }
    return if (abs(x) > significance || abs(y) > significance || abs(z) > significance) {
        Vector3d(x, y, z)
    } else null
}

fun YAMLNode.getVector3dScale(significance: Double): Vector3d? {
    val str = value ?: return null
    var x = 1.0
    var y = 1.0
    var z = 1.0
    parseYAMLxJSON(str) { key, value ->
        val parsed = value.toDoubleOrNull()
        if (parsed != null) {
            when (key) {
                "r", "x" -> x = parsed
                "g", "y" -> y = parsed
                "b", "z" -> z = parsed
            }
        }
    }
    return if (abs(x - 1) + abs(y - 1) + abs(z - 1) > significance) {
        Vector3d(x, y, z)
    } else null
}

fun YAMLNode.getQuaternion(significance: Double): Quaterniond? {
    val str = value ?: return null
    var x = 0.0
    var y = 0.0
    var z = 0.0
    var w = 1.0
    parseYAMLxJSON(str) { key, value ->
        val parsed = value.toDoubleOrNull()
        if (parsed != null) {
            when (key) {
                "r", "x" -> x = parsed
                "g", "y" -> y = parsed
                "b", "z" -> z = parsed
                "w" -> w = parsed
            }
        }
    }
    return if (abs(x) + abs(y) + abs(z) + abs(w - 1.0) > significance) {
        Quaterniond(x, y, z, w)
    } else null
}

/**
 * decodes stuff like "{fileID: 42575496, guid: ee81afb80bd, type: 2}" or {x:12, y:4, z: 13}
 * */
fun parseYAMLxJSON(json: String, callback: (key: String, value: String) -> Unit) {
    parseYAMLxJSON(json, false, callback)
}

/**
 * decodes stuff like "{fileID: 42575496, guid: ee81afb80bd, type: 2}" or {x:12, y:4, z: 13}
 * */
fun parseYAMLxJSON(json: String, beautify: Boolean, callback: (key: String, value: String) -> Unit) {
    val start = json.indexOf('{')
    if (start < 0) return
    var i = start + 1
    val length = json.length
    while (i < length) {
        while (i < length && json[i] == ' ') i++
        var colonIndex = json.indexOf(':', i + 1)
        if (colonIndex < 0) return
        var key = json.substring(i, colonIndex).trim()
        if (json[colonIndex + 1] == ' ') colonIndex++ // skip that space, that's always there
        val commaIndex = json.indexOf2(',', colonIndex + 1)
        val bracketIndex = json.indexOf2('}', colonIndex + 1)
        val endIndex = min(commaIndex, bracketIndex)
        val value = json.substring(colonIndex + 1, endIndex).trim()
        if (beautify) key = YAMLReader.beautify(key)
        callback(key, value)
        i = endIndex + 1
    }
}
