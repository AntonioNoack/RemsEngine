package me.anno.gpu.shader

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.OpenGLBuffer
import me.anno.utils.strings.StringHelper.titlecase
import me.anno.utils.structures.lists.Lists.none2
import me.anno.utils.types.Strings.iff

/**
 * functions to create compute shaders operating on OpenGLBuffers
 * */
@Suppress("unused")
object BufferCompute {

    fun createAccessors(
        given: OpenGLBuffer, wanted: List<Attribute>, suffix: String,
        binding: Int, setters: Boolean
    ): String {
        return createAccessors(given.attributes, wanted, suffix, binding, setters)
    }

    fun floatType(components: Int): String {
        return GLSLType.floats[components - 1].glslName
    }

    fun createAccessors(
        given: List<Attribute>,
        wanted: List<Attribute>,
        name: String,
        binding: Int,
        setters: Boolean
    ): String {
        if (wanted.isEmpty()) return ""
        var pos = 0
        val struct = StringBuilder()
        struct.append("struct Struct_$name {\n")
        val functions = StringBuilder()
        var padI = 0
        for (attr in given) {
            val name1 = attr.name.titlecase()
            val reqType = wanted.firstOrNull { it.name == attr.name }
            if (reqType != null) {
                struct.append("  ")
                when (attr.type) {
                    AttributeType.FLOAT -> {
                        val type = floatType(attr.components)
                        val alignment = when (attr.components) {
                            1 -> 4
                            2 -> 8
                            3 -> 16
                            4 -> 16
                            else -> throw IllegalArgumentException()
                        }
                        val size = attr.components * 4
                        if (pos % alignment != 0) throw IllegalStateException("Alignment breaks std140")
                        functions.append(
                            "$type get$name$name1(uint index){\n" +
                                    "   return $name.data[index].${attr.name};\n" +
                                    "}\n"
                        )
                        if (setters) {
                            functions.append(
                                "void set$name$name1(uint index, $type value){\n" +
                                        "   $name.data[index].${attr.name} = value;\n" +
                                        "}\n"
                            )
                        }
                        struct.append(type)
                        pos += size
                    }
                    AttributeType.SINT8_NORM -> {
                        if (attr.components != 4) throw NotImplementedError()
                        val alignment = 4
                        if (pos % alignment != 0) throw IllegalStateException("Alignment breaks std140")
                        val c = reqType.components
                        when (c) {
                            1 -> functions.append(
                                "float get$name$name1(uint index){\n" +
                                        "   int value = $name.data[index].${attr.name};\n" +
                                        "   return float((value<<24)>>24) / 127.0;\n" +
                                        "}\n"
                            )
                            2 -> functions.append(
                                "vec2 get$name$name1(uint index){\n" +
                                        "   int value = $name.data[index].${attr.name};\n" +
                                        "   return vec2((value<<24)>>24, (value<<16)>>24) / 127.0;\n" +
                                        "}\n"
                            )
                            3 -> functions.append(
                                "vec3 get$name$name1(uint index){\n" +
                                        "   int value = $name.data[index].${attr.name};\n" +
                                        "   return vec3((value<<24)>>24, (value<<16)>>24, (value<<8)>>24) / 127.0;\n" +
                                        "}\n"
                            )
                            4 -> functions.append(
                                "vec4 get$name$name1(uint index){\n" +
                                        "   int value = $name.data[index].${attr.name};\n" +
                                        "   return vec4((value<<24)>>24, (value<<16)>>24, (value<<8)>>24, value>>24) / 127.0;\n" +
                                        "}\n"
                            )
                            else -> throw NotImplementedError()
                        }
                        if (setters) {
                            functions.append(
                                "void set$name$name1(uint index, ${floatType(c)} value){\n" +
                                        "   int sum = int(round(clamp(value.x,-1.0,1.0)*127.0))&255;\n" +
                                        "   sum |= (int(round(clamp(value.y,-1.0,1.0)*127.0))&255)<<8;\n".iff(c > 1) +
                                        "   sum |= (int(round(clamp(value.z,-1.0,1.0)*127.0))&255)<<16;\n".iff(c > 2) +
                                        "   sum |= (int(round(clamp(value.w,-1.0,1.0)*127.0)))<<24;\n".iff(c > 3) +
                                        "   $name.data[index].${attr.name} = sum;\n" +
                                        "}\n"
                            )
                        }
                        struct.append("int")
                        val size = attr.components
                        pos += size
                    }
                    else -> throw NotImplementedError(attr.toString())
                }
                struct.append(' ').append(attr.name).append(";\n")
            } else {
                val size = attr.type.byteSize * attr.components
                if (size % 4 != 0) throw IllegalArgumentException()
                struct.append("  ")
                for (i in 0 until size step 4) {
                    struct.append("int pad").append(padI++).append(";")
                }
                pos += size
            }
        }
        struct.append("};\n")
        struct.append(
            "layout(std140, set = 0, binding = $binding) buffer Buffer_$name {\n" +
                    "    Struct_$name data[];\n" +
                    "} $name;\n"
        )
        for (attr in wanted) {
            if (given.none2 { it.name == attr.name }) {
                val name1 = attr.name.titlecase()
                val type = floatType(attr.components)
                functions.append(
                    "$type get$name$name1(uint index){ return $type(0.0); }\n"
                )
                if (setters) {
                    functions.append("void set$name$name1(uint index, $type value){}\n")
                }
            }
        }
        struct.append(functions)
        return struct.toString()
    }
}