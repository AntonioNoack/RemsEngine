package me.anno.io.json.generic

import me.anno.utils.types.Ints.toIntOrDefault
import me.anno.utils.types.Ints.toLongOrDefault
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Modifier

/**
 * class to write non-ISaveable() as JSON; not used anywhere
 * idk how correct/complete it is...
 * */
object ObjectMapper {

    fun asInt(value: Any?, default: Int = 0): Int {
        return when (value) {
            null -> default
            false -> 0
            true -> 1
            is Int -> value
            is Long -> value.toInt()
            is Float -> value.toInt()
            is Double -> value.toInt()
            else -> value.toString().toIntOrDefault(default)
        }
    }

    fun asLong(value: Any?, default: Long = 0L): Long {
        return when (value) {
            null -> default
            false -> 0
            true -> 1
            is Int -> value.toLong()
            is Long -> value
            is Float -> value.toLong()
            is Double -> value.toLong()
            else -> value.toString().toLongOrDefault(default)
        }
    }

    fun asFloat(value: Any?, default: Float = 0f): Float {
        return when (value) {
            null -> default
            false -> 0f
            true -> 1f
            is Int -> value.toFloat()
            is Long -> value.toFloat()
            is Float -> value
            is Double -> value.toFloat()
            else -> value.toString().toFloatOrNull() ?: default
        }
    }

    fun asDouble(value: Any?, default: Double = 0.0): Double {
        return when (value) {
            null -> default
            false -> 0.0
            true -> 1.0
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            is Float -> value.toDouble()
            is Double -> value
            else -> value.toString().toDoubleOrNull() ?: default
        }
    }

    fun asBool(value: Any?, default: Boolean = false): Boolean {
        return asInt(value, if (default) 1 else 0) != 0
    }

    fun asText(value: Any?) = value.toString()

    fun writeJsonString(output: OutputStream, str0: String) {
        val str = str0.replace("\"", "\\\"")
        output.write(str)
    }

    fun OutputStream.write(c: Char) {
        write(c.code)
    }

    fun OutputStream.write(str: String) {
        write(str.encodeToByteArray())
    }

    fun OutputStream.writeString(v: Any) {
        write(v.toString().encodeToByteArray())
    }

    fun <V> writeValue(output: OutputStream, instance: V) {
        // writing a json file from Java for copying
        val clazz = (instance as? Any)?.javaClass ?: Any::class.java
        when (clazz.typeName) {
            "int[]" -> output.write(
                (instance as? IntArray)
                    ?.joinToString(",", "[", "]") ?: "null"
            )
            "long[]" -> output.write(
                (instance as? LongArray)
                    ?.joinToString(",", "[", "]") ?: "null"
            )
            "float[]" -> output.write(
                (instance as? FloatArray)
                    ?.joinToString(",", "[", "]") ?: "null"
            )
            "double[]" -> output.write(
                (instance as? DoubleArray)
                    ?.joinToString(",", "[", "]") ?: "null"
            )
            "boolean[]" -> output.write(
                (instance as? BooleanArray)
                    ?.joinToString(",", "[", "]") ?: "null"
            )
            "java.lang.Integer" -> output.writeString((instance as Int?) ?: "null")
            "java.lang.Long" -> output.writeString((instance as Long?) ?: "null")
            "java.lang.Float" -> output.writeString((instance as Float?) ?: "null")
            "java.lang.Double" -> output.writeString((instance as Double?) ?: "null")
            else -> {
                output.write('{'.code)
                var isFirstProperty = true
                clazz.declaredFields.forEach { field ->
                    if (!Modifier.isStatic(field.modifiers)) {
                        if (!field.isAccessible) {
                            try {
                                field.isAccessible = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            // field.trySetAccessible()
                        }
                        if (!isFirstProperty) {
                            output.write(','.code)
                        }
                        output.write('"')
                        writeJsonString(output, field.name)
                        output.write('"')
                        output.write(':')
                        when (field.genericType.typeName) {
                            // native types
                            "int" -> output.writeString(field.getInt(instance))
                            "long" -> output.writeString(field.getLong(instance))
                            "float" -> output.writeString(field.getFloat(instance))
                            "double" -> output.writeString(field.getDouble(instance))
                            "boolean" -> output.writeString(field.getBoolean(instance))
                            "java.lang.Integer" -> output.writeString((field.get(instance) as Int?) ?: "null")
                            "java.lang.Long" -> output.writeString((field.get(instance) as Long?) ?: "null")
                            "java.lang.Float" -> output.writeString((field.get(instance) as Float?) ?: "null")
                            "java.lang.Double" -> output.writeString((field.get(instance) as Double?) ?: "null")
                            "int[]" -> output.write(
                                (field.get(instance) as? IntArray)
                                    ?.joinToString(",", "[", "]") ?: "null"
                            )
                            "long[]" -> output.write(
                                (field.get(instance) as? LongArray)
                                    ?.joinToString(",", "[", "]") ?: "null"
                            )
                            "float[]" -> output.write(
                                (field.get(instance) as? FloatArray)
                                    ?.joinToString(",", "[", "]") ?: "null"
                            )
                            "double[]" -> output.write(
                                (field.get(instance) as? DoubleArray)
                                    ?.joinToString(",", "[", "]") ?: "null"
                            )
                            "boolean[]" -> output.write(
                                (field.get(instance) as? BooleanArray)
                                    ?.joinToString(",", "[", "]") ?: "null"
                            )
                            // other types, including lists, arrays and maps
                            else -> {
                                when (val value = field.get(instance)) {
                                    null -> output.writeString("null")
                                    is String -> {
                                        output.write('"')
                                        writeJsonString(output, value)
                                        output.write('"')
                                    }
                                    is Array<*> -> writeArray(output, value)
                                    is List<*> -> {
                                        output.write('[')
                                        for (i in 0 until value.size) {
                                            if (i != 0) output.write(',')
                                            val vi = value[i]
                                            if (vi != null) {
                                                writeValue(output, vi)
                                            } else output.write("null")
                                        }
                                        output.write(']')
                                    }
                                    is Map<*, *> -> {
                                        output.write('{')
                                        var isFirst = true
                                        for ((key, vi) in value) {
                                            if (!isFirst) output.write(',')
                                            output.write('"')
                                            output.write(key.toString())
                                            output.write('"')
                                            output.write(':')
                                            if (vi != null) {
                                                writeValue(output, vi)
                                            } else output.write("null")
                                            isFirst = false
                                        }
                                        output.write('}')
                                    }
                                    else -> {
                                        writeValue(output, value)
                                    }
                                }
                            }
                        }
                        isFirstProperty = false
                    }
                }
                output.write('}'.code)
            }
        }
    }

    fun writeArray(output: OutputStream, value: Array<*>) {
        output.write('['.code)
        for (i in value.indices) {
            if (i != 0) output.write(',')
            when (val vi = value[i]) {
                null -> output.write("null")
                is Array<*> -> writeArray(output, vi)
                else -> writeValue(output, vi)
            }
        }
        output.write(']')
    }

    @Suppress("unused")
    fun <V> readValue(value: ByteArray, clazz: Class<V>): V {
        return convertValue(JsonReader(value).readObject(), clazz)
    }

    @Suppress("unused")
    fun <V> readValue(value: InputStream, clazz: Class<V>): V {
        return convertValue(JsonReader(value).readObject(), clazz)
    }

    fun <V> convertValue(values: HashMap<*, *>, clazz: Class<V>): V {
        val instance = clazz.getConstructor().newInstance()
        for (field in clazz.declaredFields) {
            if (!Modifier.isStatic(field.modifiers)) {
                if (!field.isAccessible) {
                    try {
                        field.isAccessible = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    // field.trySetAccessible()
                }
                val value = values[field.name]
                if (value == null) {
                    field.set(instance, null)
                } else {
                    // ("${field.name}, ${field.genericType}, ${field.genericType.typeName}")
                    val type = field.genericType.typeName
                    val theValue = getValue(parseType(type), value)
                    field.set(instance, theValue)
                }
            }
        }
        return instance
    }

    fun getValue(type: Type, value: Any?): Any {
        return if (type.arrayDimension > 0) {
            value as ArrayList<*>
            when (type.arrayDimension) {
                1 -> {
                    when (val clazzName = type.name) {
                        "float" -> FloatArray(value.size) { asFloat(value[it]) }
                        else -> {
                            val clazz = getClass(clazzName)

                            @Suppress("unchecked_cast")
                            val array = java.lang.reflect.Array.newInstance(clazz, value.size) as Array<Any>
                            val childType = type.getChild()
                            for (index in value.indices) {
                                array[index] = getValue(childType, value[index])
                            }
                            array
                        }
                    }
                }
                else -> {
                    val clazz = IntArray(0).javaClass // let's hope it doesn't matter ;)

                    @Suppress("unchecked_cast")
                    val array = java.lang.reflect.Array.newInstance(clazz, value.size) as Array<Any>
                    val childType = type.getChild()
                    for (index in value.indices) {
                        array[index] = getValue(childType, value[index])
                    }
                    array
                }
            }
        } else {
            when (type.name) {
                "java.lang.Integer" -> asInt(value)
                "java.lang.Long" -> asLong(value)
                "java.lang.Float" -> asFloat(value)
                "java.lang.Double" -> asDouble(value)
                "java.lang.String" -> asText(value)
                "java.lang.Number" -> asDouble(value)
                "java.lang.Boolean" -> asBool(value)
                "java.util.List" -> {
                    value as ArrayList<*>
                    val val2 = ArrayList<Any>()
                    val contentType = type.generics[0]
                    for (it in value) {
                        val2.add(getValue(contentType, it))
                    }
                    val2
                }
                "java.util.Map" -> {
                    value as HashMap<*, *>
                    val val2 = HashMap<String, Any>()
                    val contentType = type.generics[1]
                    for ((key, val3) in value) {
                        val2[key as String] = getValue(contentType, val3)
                    }
                    val2
                }
                else -> {
                    // (type)
                    val clazz = getClass(type.name)
                    convertValue(value as HashMap<*, *>, clazz)
                }
            }
        }
    }

    class Type(val name: String, val arrayDimension: Int, val generics: List<Type>) {
        fun getChild() = Type(name, arrayDimension - 1, generics)
        override fun toString() = "$name${generics.joinToString(", ", "<", ">")}^$arrayDimension"
    }

    fun String.betterIndexOf(key: Char): Int {
        val io = indexOf(key)
        return if (io < 0) length
        else io
    }

    fun parseType(value: String, dimension: Int = 0): Type {
        if (value.endsWith("[]")) return parseType(value.substring(0, value.length - 2).trim(), dimension + 1)
        val next = value.betterIndexOf('<')
        if (next >= value.length) return Type(value.trim(), dimension, emptyList())
        val generics = ArrayList<Type>()
        var i0 = next + 1
        var i = next + 1
        var depth = 1
        // a<b,c>
        while (i < value.length && depth > 0) {
            when (value[i]) {
                '<' -> depth++
                ',' -> {
                    if (depth == 1) {
                        // bottom -> add the type to the generic list
                        generics += parseType(value.substring(i0, i).trim())
                        i0 = i + 1 // one more will come at least
                    }
                }
                '>' -> {
                    if (depth == 1) {
                        // bottom -> add the type to the generic list
                        generics += parseType(value.substring(i0, i))
                    }
                    depth--
                }
            }
            i++
        }
        return Type(value.substring(0, next).trim(), dimension, generics)
    }

    fun getClass(name: String): Class<*> {
        val cached = classes[name]
        if (cached != null) return cached
        val clazz = javaClass.classLoader.loadClass(name)
        classes[name] = clazz
        return clazz
    }

    val classes = HashMap<String, Class<*>>()

}