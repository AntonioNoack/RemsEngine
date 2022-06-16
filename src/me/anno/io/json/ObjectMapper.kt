package me.anno.io.json

import me.anno.utils.JavaUtils
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Modifier

object ObjectMapper {

    fun writeJsonString(output: OutputStream, str: String){
        output.write(
            str
                .replace("\"", "\\\"")
                .toByteArray()
        )
    }

    fun OutputStream.write(c: Char){
        write(c.code)
    }

    fun OutputStream.write(str: String){
        write(str.toByteArray())
    }

    fun OutputStream.writeStr(v: Any){
        write(v.toString().toByteArray())
    }

    @Throws(IOException::class)
    fun <V> writeValue(output: OutputStream, instance: V){
        // writing a json file from Java for copying
        val clazz = JavaUtils.getClass(instance)
        when(clazz.typeName){
            "int[]" -> output.write((instance as? IntArray)
                ?.joinToString(",", "[", "]") ?: "null")
            "long[]" -> output.write((instance as? LongArray)
                ?.joinToString(",", "[", "]") ?: "null")
            "float[]" -> output.write((instance as? FloatArray)
                ?.joinToString(",", "[", "]") ?: "null")
            "double[]" -> output.write((instance as? DoubleArray)
                ?.joinToString(",", "[", "]") ?: "null")
            "boolean[]" -> output.write((instance as? BooleanArray)
                ?.joinToString(",", "[", "]") ?: "null")
            "java.lang.Integer" -> output.writeStr((instance as Int?) ?: "null")
            "java.lang.Long" ->    output.writeStr((instance as Long?) ?: "null")
            "java.lang.Float" ->   output.writeStr((instance as Float?) ?: "null")
            "java.lang.Double" ->  output.writeStr((instance as Double?) ?: "null")
            else -> {
                output.write('{'.code)
                var isFirstProperty = true
                clazz.declaredFields.forEach { field ->
                    if(!Modifier.isStatic(field.modifiers)){
                        if(!field.isAccessible){
                            try {
                                field.isAccessible = true
                            } catch (e: Exception){ e.printStackTrace() }
                            // field.trySetAccessible()
                        }
                        if(!isFirstProperty){ output.write(','.code) }
                        output.write('"')
                        writeJsonString(output, field.name)
                        output.write('"')
                        output.write(':')
                        when(field.genericType.typeName){
                            // native types
                            "int" -> output.writeStr(field.getInt(instance))
                            "long" -> output.writeStr(field.getLong(instance))
                            "float" -> output.writeStr(field.getFloat(instance))
                            "double" -> output.writeStr(field.getDouble(instance))
                            "boolean" -> output.writeStr(field.getBoolean(instance))
                            "java.lang.Integer" -> output.writeStr((field.get(instance) as Int?) ?: "null")
                            "java.lang.Long" -> output.writeStr((field.get(instance) as Long?) ?: "null")
                            "java.lang.Float" -> output.writeStr((field.get(instance) as Float?) ?: "null")
                            "java.lang.Double" -> output.writeStr((field.get(instance) as Double?) ?: "null")
                            "int[]" -> output.write((field.get(instance) as? IntArray)
                                ?.joinToString(",", "[", "]") ?: "null")
                            "long[]" -> output.write((field.get(instance) as? LongArray)
                                ?.joinToString(",", "[", "]") ?: "null")
                            "float[]" -> output.write((field.get(instance) as? FloatArray)
                                ?.joinToString(",", "[", "]") ?: "null")
                            "double[]" -> output.write((field.get(instance) as? DoubleArray)
                                ?.joinToString(",", "[", "]") ?: "null")
                            "boolean[]" -> output.write((field.get(instance) as? BooleanArray)
                                ?.joinToString(",", "[", "]") ?: "null")
                            // other types, including lists, arrays and maps
                            else -> {
                                when(val value = field.get(instance)){
                                    null -> output.write("null".toByteArray())
                                    is String -> {
                                        output.write('"')
                                        writeJsonString(output, value)
                                        output.write('"')
                                    }
                                    is Array<*> -> writeArray(output, value)
                                    is List<*> -> {
                                        output.write('[')
                                        for(i in 0 until value.size){
                                            if(i != 0) output.write(',')
                                            val vi = value[i]
                                            if(vi != null) {
                                                writeValue(output, vi)
                                            } else output.write("null")
                                        }
                                        output.write(']')
                                    }
                                    is Map<*, *> -> {
                                        output.write('{')
                                        var isFirst = true
                                        for((key, vi) in value){
                                            if(!isFirst) output.write(',')
                                            output.write('"')
                                            output.write(key.toString())
                                            output.write('"')
                                            output.write(':')
                                            if(vi != null) {
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

    fun writeArray(output: OutputStream, value: Array<*>){
        output.write('['.code)
        for(i in value.indices){
            if(i != 0) output.write(',')
            when(val vi = value[i]){
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

    fun <V> convertValue(values: JsonNode, clazz: Class<V>): V {
        val instance = clazz.getConstructor().newInstance()
        clazz.declaredFields.forEach { field ->
            if(!Modifier.isStatic(field.modifiers)){
                if(!field.isAccessible){
                    try {
                        field.isAccessible = true
                    } catch (e: Exception){ e.printStackTrace() }
                    // field.trySetAccessible()
                }
                val value = values.get(field.name)
                if(value == null){
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

    fun getValue(type: Type, value: JsonNode): Any {
        return if(type.arrayDimension > 0){
            value as JsonArray
            when(type.arrayDimension){
                1 -> {
                    when(val clazzName = type.name){
                        "float" -> FloatArray(value.content.size){
                            value.content[it].toString().toFloat()
                        }
                        else -> {
                            val clazz = getClass(clazzName)
                            @Suppress("unchecked_cast")
                            val array = java.lang.reflect.Array.newInstance(clazz, value.content.size) as Array<Any>
                            val childType = type.getChild()
                            value.content.forEachIndexed { index, any ->
                                array[index] = getValue(childType, any.toJsonNode())
                            }
                            array
                        }
                    }
                }
                else -> {
                    val clazz = IntArray(0).javaClass // let's hope it doesn't matter ;)
                    @Suppress("unchecked_cast")
                    val array = java.lang.reflect.Array.newInstance(clazz, value.content.size) as Array<Any>
                    val childType = type.getChild()
                    value.content.forEachIndexed { index, any ->
                        array[index] = getValue(childType, any.toJsonNode())
                    }
                    array
                }
            }
        } else {
            when(type.name) {
                "java.lang.Integer" -> value.asInt()
                "java.lang.Long" -> value.asLong()
                "java.lang.Float" -> value.asFloat()
                "java.lang.Double" -> value.asDouble()
                "java.lang.String" -> value.asText()
                "java.lang.Number" -> value.asDouble()
                "java.lang.Boolean" -> value.asBool()
                "java.util.List" -> {
                    value as JsonArray
                    val val2 = ArrayList<Any>()
                    val contentType = type.generics[0]
                    value.content.forEach {
                        val2.add(getValue(contentType, it.toJsonNode()))
                    }
                    val2
                }
                "java.util.Map" -> {
                    value as JsonObject
                    val val2 = HashMap<String, Any>()
                    val contentType = type.generics[1]
                    for ((key, val3) in value.map){
                        val2[key] = getValue(contentType, val3.toJsonNode())
                    }
                    val2
                }
                else -> {
                    // (type)
                    val clazz = getClass(type.name)
                    convertValue(value.toJsonNode(), clazz)
                }
            }
        }
    }

    class Type(val name: String, val arrayDimension: Int, val generics: List<Type>){
        fun getChild() = Type(name, arrayDimension-1, generics)
        override fun toString() = "$name${generics.joinToString(", ", "<", ">")}^$arrayDimension"
    }

    fun String.betterIndexOf(key: Char): Int {
        val io = indexOf(key)
        return if(io < 0) length
        else io
    }

    fun parseType(value: String, dimension: Int = 0): Type {
        if(value.endsWith("[]")) return parseType(value.substring(0, value.length-2).trim(), dimension+1)
        val next = value.betterIndexOf('<')
        if(next >= value.length) return Type(value.trim(), dimension, emptyList())
        val generics = ArrayList<Type>()
        var i0 = next+1
        var i = next+1
        var depth = 1
        // a<b,c>
        while(i < value.length && depth > 0){
            when(value[i]){
                '<' -> depth++
                ',' -> {
                    if(depth == 1){
                        // bottom -> add the type to the generic list
                        generics += parseType(value.substring(i0, i).trim())
                        i0 = i+1 // one more will come at least
                    }
                }
                '>' -> {
                    if(depth == 1){
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
        if(cached != null) return cached
        val clazz = javaClass.classLoader.loadClass(name)
        classes[name] = clazz
        return clazz
    }

    @Suppress("unused")
    fun Any?.toJsonNode(): JsonNode {
        val value = this
        if(value is JsonNode) return value
        return JsonValue(value)
    }

    val classes = HashMap<String, Class<*>>()

}