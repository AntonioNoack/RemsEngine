package me.anno.gpu.shader

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeLayout
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.OpenGLBuffer
import me.anno.utils.types.Strings.iff
import me.anno.utils.types.Strings.titlecase

/**
 * functions to create compute shaders operating on OpenGLBuffers
 * */
@Suppress("unused")
object BufferCompute {

    fun createAccessors(
        given: OpenGLBuffer, wanted: List<Attribute>, name: String,
        binding: Int, setters: Boolean
    ): String {
        return createAccessors(given.attributes, wanted, name, binding, setters)
    }

    private fun floatType(components: Int): String {
        return GLSLType.floats[components - 1].glslName
    }

    private fun uintType(components: Int): String {
        return when (components) {
            1 -> "uint"
            2 -> "uvec2"
            3 -> "uvec3"
            4 -> "uvec4"
            else -> throw IllegalArgumentException("Unsupported #components")
        }
    }

    private fun sintType(components: Int): String {
        return GLSLType.integers[components - 1].glslName
    }

    private fun floatAlignment(components: Int): Int {
        return when (components) {
            1 -> 4
            2 -> 8
            3 -> 16
            4 -> 16
            else -> throw IllegalArgumentException("Unsupported #components")
        }
    }

    fun createAccessors(
        given: AttributeLayout,
        wanted: List<Attribute>,
        name: String, binding: Int,
        setters: Boolean
    ): String {
        if (wanted.isEmpty()) return ""
        var pos = 0
        val result = StringBuilder()
        result.append(
            // 430 = compact
            "layout(std430, set = 0, binding = $binding) buffer Buffer$name {\n" +
                    "    uint data[];\n" +
                    "} $name;\n"
        )
        val stride4 = given.stride shr 2
        for (attr in 0 until given.size) {
            val offset4 = pos shr 2
            val attrName = given.name(attr)
            val name1 = attrName.titlecase()
            val reqType = wanted.firstOrNull { it.name == attrName }
            val numAttrComponents = given.components(attr)
            if (reqType != null) {
                when (given.type(attr)) {
                    AttributeType.FLOAT -> {
                        val type = floatType(numAttrComponents)
                        val alignment = floatAlignment(numAttrComponents)
                        val size = numAttrComponents * 4
                        if (pos % alignment != 0) {
                            throw IllegalStateException("Alignment breaks std430: $given, $pos % $alignment")
                        }
                        result.append(
                            "$type get$name$name1(uint index){\n" +
                                    "   uint idx = index*${stride4}+$offset4;\n" +
                                    when (numAttrComponents) {
                                        1 -> "return uintBitsToFloat($name.data[idx]);\n"
                                        2 -> "return vec2(uintBitsToFloat($name.data[idx]),uintBitsToFloat($name.data[idx+1]));\n"
                                        3 -> "return vec3(uintBitsToFloat($name.data[idx]),uintBitsToFloat($name.data[idx+1]),uintBitsToFloat($name.data[idx+2]));\n"
                                        4 -> "return vec4(uintBitsToFloat($name.data[idx]),uintBitsToFloat($name.data[idx+1]),uintBitsToFloat($name.data[idx+2]),uintBitsToFloat($name.data[idx+3]));\n"
                                        else -> throw NotImplementedError()
                                    } +
                                    "}\n"
                        )
                        if (setters) {
                            result.append(
                                "void set$name$name1(uint index, $type value){\n" +
                                        "   uint idx = index*${stride4}+$offset4;\n" +
                                        (0 until numAttrComponents).joinToString("") {
                                            "$name.data[idx+$it] = floatBitsToUint(value.${"xyzw"[it]});\n"
                                        } +
                                        "}\n"
                            )
                        }
                        pos += size
                    }
                    AttributeType.UINT32 -> {
                        val type = uintType(numAttrComponents)
                        val alignment = floatAlignment(numAttrComponents)
                        val size = numAttrComponents * 4
                        if (pos % alignment != 0) {
                            throw IllegalStateException("Alignment breaks std430: $given, $pos % $alignment")
                        }
                        result.append(
                            "$type get$name$name1(uint index){\n" +
                                    "   uint idx = index*${stride4}+$offset4;\n" +
                                    when (numAttrComponents) {
                                        1 -> "return ($name.data[idx]);\n"
                                        2 -> "return uvec2(($name.data[idx]),($name.data[idx+1]));\n"
                                        3 -> "return uvec3(($name.data[idx]),($name.data[idx+1]),($name.data[idx+2]));\n"
                                        4 -> "return uvec4(($name.data[idx]),($name.data[idx+1]),($name.data[idx+2]),($name.data[idx+3]));\n"
                                        else -> throw NotImplementedError()
                                    } +
                                    "}\n"
                        )
                        if (setters) {
                            result.append(
                                "void set$name$name1(uint index, $type value){\n" +
                                        "   uint idx = index*${stride4}+$offset4;\n" +
                                        (0 until numAttrComponents).joinToString("") {
                                            "$name.data[idx+$it] = (value.${"xyzw"[it]})\n"
                                        } +
                                        "}\n"
                            )
                        }
                        pos += size
                    }
                    AttributeType.SINT32 -> {
                        val type = sintType(numAttrComponents)
                        val alignment = floatAlignment(numAttrComponents)
                        val size = numAttrComponents * 4
                        if (pos % alignment != 0) {
                            throw IllegalStateException("Alignment breaks std430: $given, $pos % $alignment")
                        }
                        result.append(
                            "$type get$name$name1(uint index){\n" +
                                    "   uint idx = index*${stride4}+$offset4;\n" +
                                    when (numAttrComponents) {
                                        1 -> "return int($name.data[idx]);\n"
                                        2 -> "return ivec2(int($name.data[idx]),int($name.data[idx+1]));\n"
                                        3 -> "return ivec3(int($name.data[idx]),int($name.data[idx+1]),int($name.data[idx+2]));\n"
                                        4 -> "return ivec4(int($name.data[idx]),int($name.data[idx+1]),int($name.data[idx+2]),int($name.data[idx+3]));\n"
                                        else -> throw NotImplementedError()
                                    } +
                                    "}\n"
                        )
                        if (setters) {
                            result.append(
                                "void set$name$name1(uint index, $type value){\n" +
                                        "   uint idx = index*${stride4}+$offset4;\n" +
                                        (0 until numAttrComponents).joinToString("") {
                                            "$name.data[idx+$it] = uint(value.${"xyzw"[it]})\n"
                                        } +
                                        "}\n"
                            )
                        }
                        pos += size
                    }
                    AttributeType.SINT8_NORM -> {
                        if (numAttrComponents != 4) throw NotImplementedError()
                        val alignment = 4
                        if (pos % alignment != 0) throw IllegalStateException("Alignment breaks std140")
                        val c = reqType.numComponents
                        when (c) {
                            1 -> result.append(
                                "float get$name$name1(uint index){\n" +
                                        "   uint idx = index*${stride4}+$offset4;\n" +
                                        "   int value = int($name.data[idx]);\n" +
                                        "   return float((value<<24)>>24) / 127.0;\n" +
                                        "}\n"
                            )
                            2 -> result.append(
                                "vec2 get$name$name1(uint index){\n" +
                                        "   uint idx = index*${stride4}+$offset4;\n" +
                                        "   int value = int($name.data[idx]);\n" +
                                        "   return vec2((value<<24)>>24, (value<<16)>>24) / 127.0;\n" +
                                        "}\n"
                            )
                            3 -> result.append(
                                "vec3 get$name$name1(uint index){\n" +
                                        "   uint idx = index*${stride4}+$offset4;\n" +
                                        "   int value = int($name.data[idx]);\n" +
                                        "   return vec3((value<<24)>>24, (value<<16)>>24, (value<<8)>>24) / 127.0;\n" +
                                        "}\n"
                            )
                            4 -> result.append(
                                "vec4 get$name$name1(uint index){\n" +
                                        "   uint idx = index*${stride4}+$offset4;\n" +
                                        "   int value = int($name.data[idx]);\n" +
                                        "   return vec4((value<<24)>>24, (value<<16)>>24, (value<<8)>>24, value>>24) / 127.0;\n" +
                                        "}\n"
                            )
                            else -> throw NotImplementedError()
                        }
                        if (setters) {
                            result.append(
                                "void set$name$name1(uint index, ${floatType(c)} value){\n" +
                                        "   int sum = int(round(clamp(value.x,-1.0,1.0)*127.0))&255;\n" +
                                        "   sum |= (int(round(clamp(value.y,-1.0,1.0)*127.0))&255)<<8;\n".iff(c > 1) +
                                        "   sum |= (int(round(clamp(value.z,-1.0,1.0)*127.0))&255)<<16;\n".iff(c > 2) +
                                        "   sum |= (int(round(clamp(value.w,-1.0,1.0)*127.0)))<<24;\n".iff(c > 3) +
                                        "   uint idx = index*${stride4}+$offset4;\n" +
                                        "   $name.data[idx] = uint(sum);\n" +
                                        "}\n"
                            )
                        }
                        val size = numAttrComponents
                        pos += size
                    }
                    AttributeType.UINT8_NORM -> {
                        if (numAttrComponents != 4) throw NotImplementedError()
                        val alignment = 4
                        if (pos % alignment != 0) throw IllegalStateException("Alignment breaks std430")
                        val c = reqType.numComponents
                        when (c) {
                            1 -> result.append(
                                "float get$name$name1(uint index){\n" +
                                        "   uint idx = index*${stride4}+$offset4;\n" +
                                        "   uint value = $name.data[idx];\n" +
                                        "   return float(value & 255) / 255.0;\n" +
                                        "}\n"
                            )
                            2 -> result.append(
                                "vec2 get$name$name1(uint index){\n" +
                                        "   uint idx = index*${stride4}+$offset4;\n" +
                                        "   uint value = $name.data[idx];\n" +
                                        "   return vec2((value<<24)>>24, (value<<16)>>24) / 255.0;\n" +
                                        "}\n"
                            )
                            3 -> result.append(
                                "vec3 get$name$name1(uint index){\n" +
                                        "   uint idx = index*${stride4}+$offset4;\n" +
                                        "   uint value = $name.data[idx];\n" +
                                        "   return vec3((value<<24)>>24, (value<<16)>>24, (value<<8)>>24) / 255.0;\n" +
                                        "}\n"
                            )
                            4 -> result.append(
                                "vec4 get$name$name1(uint index){\n" +
                                        "   uint idx = index*${stride4}+$offset4;\n" +
                                        "   uint value = $name.data[idx];\n" +
                                        "   return vec4((value<<24)>>24, (value<<16)>>24, (value<<8)>>24, value>>24) / 255.0;\n" +
                                        "}\n"
                            )
                            else -> throw NotImplementedError()
                        }
                        if (setters) {
                            result.append(
                                "void set$name$name1(uint index, ${floatType(c)} value){\n" +
                                        "   uint sum = uint(round(clamp(value.x,0.0,1.0)*255.0))&255;\n" +
                                        "   sum |= (uint(round(clamp(value.y,0.0,1.0)*255.0))&255)<<8;\n".iff(c > 1) +
                                        "   sum |= (uint(round(clamp(value.z,0.0,1.0)*255.0))&255)<<16;\n".iff(c > 2) +
                                        "   sum |= (uint(round(clamp(value.w,0.0,1.0)*255.0)))<<24;\n".iff(c > 3) +
                                        "   uint idx = index*${stride4}+$offset4;\n" +
                                        "   $name.data[idx] = sum;\n" +
                                        "}\n"
                            )
                        }
                        val size = numAttrComponents
                        pos += size
                    }
                    else -> throw NotImplementedError(attr.toString())
                }
            }
        }
        // append missing attributes as 0 or (0,0,0,0)
        for (i in wanted.indices) {
            val attr = wanted[i]
            if (given.indexOf(attr.name) < 0) {
                val name1 = attr.name.titlecase()
                val type = floatType(attr.numComponents)
                result.append("$type get$name$name1(uint index){ return $type(0.0); }\n")
                if (setters) {
                    result.append("void set$name$name1(uint index, $type value){}\n")
                }
            }
        }
        return result.toString()
    }
}