package me.anno.io.json

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception

object ObjectMapper {

    @Throws(IOException::class)
    fun <V> writeValue(output: OutputStream, value: V){
        TODO("Implement writing a json file from Java for copying")
    }

    fun <V> readValue(value: ByteArray, clazz: Class<V>): V {
        return convertValue(JsonReader(value.inputStream()).readObject(), clazz)
    }

    fun <V> readValue(value: InputStream, clazz: Class<V>): V {
        return convertValue(JsonReader(value).readObject(), clazz)
    }

    fun <V> convertValue(values: JsonNode, clazz: Class<V>): V {
        val instance = clazz.getConstructor().newInstance()
        clazz.declaredFields.forEach { field ->
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
                // println("${field.name}, ${field.genericType}, ${field.genericType.typeName}")
                val type = field.genericType.typeName
                val theValue = getValue(parseType(type), value)
                field.set(instance, theValue)
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
                "java.lang.Integer" -> value.asText().toInt()
                "java.lang.Long" -> value.asText().toLong()
                "java.lang.Float" -> value.asText().toFloat()
                "java.lang.Double" -> value.asText().toDouble()
                "java.lang.String" -> value.asText()
                "java.lang.Number" -> value.asText().toDouble()
                "java.lang.Boolean" -> value.asText().toBoolean()
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
                    // println(type)
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

    fun Any.toJsonNode(): JsonNode {
        val value = this
        if(value is JsonNode) return value
        return JsonValue(value)
    }

    val classes = HashMap<String, Class<*>>()

}