package me.anno.gpu.buffer

import me.anno.gpu.shader.GLSLType
import me.anno.utils.assertions.assertFalse
import me.anno.utils.types.Strings.titlecase

/**
 * Creates shader functions to read/write OpenGL Buffers.
 *
 * GLSL isn't good enough afaik to define structures like attributes can be defined.
 * This creates helper functions to read and write the attributes properly anyway.
 * */
object AttributeReadWrite {

    fun createAccessors(
        given: OpenGLBuffer, wanted: List<Attribute>, name: String,
        binding: Int, setters: Boolean
    ): String {
        return createAccessors(given.attributes, wanted, name, binding, setters)
    }

    private fun floatType(components: Int): String {
        return GLSLType.Companion.floats[components - 1].glslName
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
        return GLSLType.Companion.integers[components - 1].glslName
    }

    private fun floatAlignment(components: Int): Int {
        return when (components) {
            1 -> 4
            2 -> 8
            3, 4 -> 16
            else -> throw IllegalArgumentException("Unsupported #components")
        }
    }

    private fun StringBuilder.appendGetterHeader(
        type: String, name: String, name1: CharSequence
    ): StringBuilder {
        append(type).append(" get").append(name).append(name1)
        append("(uint index) {\n")
        return this
    }

    private fun StringBuilder.appendSetterHeader(
        type: String, name: String, name1: CharSequence
    ): StringBuilder {
        append("void set").append(name).append(name1)
        append("(uint index, ").append(type).append(" value) {\n")
        return this
    }

    private fun StringBuilder.appendIdx(stride: Int, offset: Int): StringBuilder {
        append("uint idx = index")
        if (stride != 1) append('*').append(stride).append('u')
        if (offset != 0) append('+').append(offset).append('u')
        append(";\n")
        return this
    }

    fun createAccessors(
        given: AttributeLayout,
        wantedList: List<Attribute>,
        name: String, binding: Int,
        setters: Boolean
    ): String {
        if (wantedList.isEmpty()) return ""
        val result = StringBuilder()
        appendAccessorsHeader(name, binding, setters, result)
        // append missing attributes as 0 or (0,0,0,0)
        for (i in wantedList.indices) {
            val wantedI = wantedList[i]
            val name1 = wantedI.name.titlecase()
            val givenIndex = given.indexOf(wantedI.name)
            if (givenIndex >= 0) {
                appendDataAccessor(name, name1, given, givenIndex, setters, result)
            } else {
                appendVoidAccessor(name, name1, setters, wantedI.glslType, result)
            }
        }
        return result.toString()
    }

    fun appendAccessorsHeader(name: String, binding: Int, setters: Boolean, result: StringBuilder) {
        // 430 = compact; doesn't matter for uint[]
        result.append("layout(std430, binding = ").append(binding).append(") ")
        if (!setters) result.append("readonly ")
        result.append("buffer Buffer").append(name).append(" { uint data[]; } ").append(name).append(";\n")
    }

    fun appendDataAccessor(
        name: String, name1: CharSequence,
        given: AttributeLayout, givenIndex: Int,
        setters: Boolean, result: StringBuilder
    ) {
        val stride1 = given.stride(givenIndex)
        val stride2 = stride1 shr 1
        val stride4 = stride1 shr 2
        val offset1 = given.offset(givenIndex)
        val offset2 = offset1 shr 1
        val offset4 = offset1 shr 2
        val c = given.components(givenIndex)
        when (given.type(givenIndex)) {
            // todo implement half and maybe double
            AttributeType.FLOAT -> {
                val type = floatType(c)
                val alignment = floatAlignment(c)
                if (offset1 % alignment != 0) {
                    throw IllegalStateException("Alignment breaks std430: $given, $offset1 % $alignment")
                }
                result.appendGetterHeader(type, name, name1).appendIdx(stride4, offset4)
                result.append("return ").append(type).append("(")
                result.append("uintBitsToFloat($name.data[idx])")
                if (c > 1) result.append(", uintBitsToFloat($name.data[idx+1u])")
                if (c > 2) result.append(", uintBitsToFloat($name.data[idx+2u])")
                if (c > 3) result.append(", uintBitsToFloat($name.data[idx+3u])")
                result.append("); }\n")
                if (setters) {
                    result.appendSetterHeader(type, name, name1).appendIdx(stride4, offset4)
                    for (i in 0 until c) {
                        result.append(name).append(".data[idx+").append(i).append("u] = floatBitsToUint(value")
                            .append(mask(c, i)).append(");\n")
                    }
                    result.append("}\n")
                }
            }
            // todo implement normalized uint32
            AttributeType.UINT32 -> {
                val type = uintType(c)
                createInt32Accessor(
                    offset1, c, given, result, name, name1,
                    stride4, offset4, setters, type
                )
            }
            AttributeType.SINT32 -> {
                val type = sintType(c)
                createInt32Accessor(
                    offset1, c, given, result, name, name1,
                    stride4, offset4, setters, type
                )
            }
            AttributeType.SINT8 -> createInt8Accessor(
                offset1, c, given, givenIndex, result, name, name1,
                stride4, offset4, false, setters, false
            )
            AttributeType.UINT8 -> createInt8Accessor(
                offset1, c, given, givenIndex, result, name, name1,
                stride4, offset4, false, setters, true
            )
            AttributeType.SINT8_NORM -> createInt8Accessor(
                offset1, c, given, givenIndex, result, name, name1,
                stride4, offset4, true, setters, false
            )
            AttributeType.UINT8_NORM -> createInt8Accessor(
                offset1, c, given, givenIndex, result, name, name1,
                stride4, offset4, true, setters, true
            )
            AttributeType.SINT16 -> createInt16Accessor(
                offset1, c, given, givenIndex, result, name, name1,
                stride2, offset2, false, setters, false
            )
            AttributeType.UINT16 -> createInt16Accessor(
                offset1, c, given, givenIndex, result, name, name1,
                stride2, offset2, false, setters, true
            )
            AttributeType.SINT16_NORM -> createInt16Accessor(
                offset1, c, given, givenIndex, result, name, name1,
                stride2, offset2, true, setters, false
            )
            AttributeType.UINT16_NORM -> createInt16Accessor(
                offset1, c, given, givenIndex, result, name, name1,
                stride2, offset2, true, setters, true
            )
            else -> throw NotImplementedError(givenIndex.toString())
        }
    }

    fun appendVoidAccessor(
        name: String, name1: CharSequence, setters: Boolean,
        glslType: GLSLType, result: StringBuilder
    ) {
        assertFalse(glslType.isSampler)
        val type = glslType.glslName
        result.append("$type get$name$name1(uint index){ return ")
        appendVoidValue(glslType, result)
        result.append("; }\n")
        if (setters) {
            result.append("void set$name$name1(uint index, $type value){}\n")
        }
    }

    fun appendVoidValue(glslType: GLSLType, result: StringBuilder) {
        assertFalse(glslType.isSampler)
        val type = glslType.glslName
        val defaultValue = when (glslType) {
            GLSLType.V1B, GLSLType.V2B, GLSLType.V3B, GLSLType.V4B -> "false"
            GLSLType.V1I, GLSLType.V2I, GLSLType.V3I, GLSLType.V4I -> "0"
            else -> "0.0"
        }
        result.append("$type($defaultValue)")
    }

    private fun mask(c: Int, i: Int): String {
        if (c == 1) return ""
        return when (i) {
            0 -> ".x"
            1 -> ".y"
            2 -> ".z"
            else -> ".w"
        }
    }

    private fun createInt8Accessor(
        pos: Int, c: Int, given: AttributeLayout, attr: Int,
        result: StringBuilder, name: String, name1: CharSequence,
        stride: Int, offset: Int, normalized: Boolean, setters: Boolean, unsigned: Boolean
    ) {
        if (pos.and(3) != 0) {
            throw NotImplementedError("Unsupported offset $pos for ${given.type(attr)}")
        }
        val type = when {
            normalized -> floatType(c)
            unsigned -> uintType(c)
            else -> sintType(c)
        }
        result.appendGetterHeader(type, name, name1).appendIdx(stride, offset)
        result.append(if (unsigned) "uint value = (" else "int value = int(").append(name).append(".data[idx]);\n")
        result.append("return ").append(type).append("((value<<24)>>24")
        if (c > 1) result.append(", (value<<16)>>24")
        if (c > 2) result.append(", (value<<8)>>24")
        if (c > 3) result.append(", (value)>>24")
        result.append(")")
        if (normalized) result.append("*").append(if (unsigned) INV255 else INV127)
        result.append(";\n}\n")
        if (setters) {
            result.appendSetterHeader(type, name, name1).appendIdx(stride, offset)
            val m0 = mask(c, 0)
            if (normalized) {
                if (unsigned) {
                    result.append("uint sum = uint(round(clamp(value$m0,0.0,1.0)*255.0)) & 255;\n")
                    if (c > 1) result.append("sum |= (uint(round(clamp(value.y,0.0,1.0)*255.0)) & 255)<<8;\n")
                    if (c > 2) result.append("sum |= (uint(round(clamp(value.z,0.0,1.0)*255.0)) & 255)<<16;\n")
                    if (c > 3) result.append("sum |= (uint(round(clamp(value.w,0.0,1.0)*255.0)) & 255)<<24;\n")
                } else {
                    result.append("int sum = int(round(clamp(value$m0,-1.0,1.0)*127.0)) & 255;\n")
                    if (c > 1) result.append("sum |= (int(round(clamp(value.y,-1.0,1.0)*127.0)) & 255)<<8;\n")
                    if (c > 2) result.append("sum |= (int(round(clamp(value.z,-1.0,1.0)*127.0)) & 255)<<16;\n")
                    if (c > 3) result.append("sum |= (int(round(clamp(value.w,-1.0,1.0)*127.0)) & 255)<<24;\n")
                }
            } else {
                if (unsigned) {
                    result.append("uint sum = value$m0 & 255u;\n")
                    if (c > 1) result.append("sum |= (value.y & 255u)<<8;\n")
                    if (c > 2) result.append("sum |= (value.z & 255u)<<16;\n")
                    if (c > 3) result.append("sum |= (value.w & 255u)<<24;\n")
                } else {
                    result.append("int sum = value$m0 & 255;\n")
                    if (c > 1) result.append("sum |= (value.y & 255)<<8;\n")
                    if (c > 2) result.append("sum |= (value.z & 255)<<16;\n")
                    if (c > 3) result.append("sum |= (value.w & 255)<<24;\n")
                }
            }
            if (unsigned) {
                result.append(name).append(".data[idx] = sum;\n}\n")
            } else {
                result.append(name).append(".data[idx] = uint(sum);\n}\n")
            }
        }
    }

    private fun createInt16Accessor(
        pos: Int, c: Int, given: AttributeLayout, attr: Int,
        result: StringBuilder, name: String, name1: CharSequence,
        stride: Int, offset: Int, normalized: Boolean, setters: Boolean, unsigned: Boolean
    ) {
        if (pos.and(1) != 0) {
            throw NotImplementedError("Unsupported offset $pos for ${given.type(attr)}")
        }
        val type = when {
            normalized -> floatType(c)
            unsigned -> uintType(c)
            else -> sintType(c)
        }
        val joined = "$name$name1"
        createInt16Helper(result, joined)
        result.appendGetterHeader(type, name, name1).appendIdx(stride, offset)
        result.append("return ").append(type).append("getU16").append(joined).append("(idx)")
        if (c > 1) result.append(", ").append("getU16").append(joined).append("(idx+1u)")
        if (c > 2) result.append(", ").append("getU16").append(joined).append("(idx+2u)")
        if (c > 3) result.append(", ").append("getU16").append(joined).append("(idx+3u)")
        result.append(")")
        if (normalized) result.append("*").append(if (unsigned) INV65535 else INV32767)
        result.append(";\n}\n")
        if (setters) {
            result.appendSetterHeader(type, name, name1).appendIdx(stride, offset)
            result.append("uvec4 tmp = uvec4(0u);\n")
            for (i in 0 until c) {
                val mask = mask(c, i)
                result.append("tmp").append(mask).append(" = ")
                if (normalized) {
                    if (unsigned) {
                        result.append("uint(round(clamp(value$mask,0.0,1.0)*65535.0));\n")
                    } else {
                        result.append("uint(int(round(clamp(value$mask,-1.0,1.0)*32767.0)));\n")
                    }
                } else {
                    if (unsigned) {
                        result.append("value$mask;\n")
                    } else {
                        result.append("uint(value$mask);\n")
                    }
                }
            }
            result.append("tmp = tmp & uvec4(0xffffu);\n")
            result.append("setU16").append(joined).append("(idx,tmp")
            result.append(
                when (c) {
                    1 -> ".x"
                    2 -> ".xy"
                    3 -> ".xyz"
                    else -> ""
                }
            ).append(");\n}\n")
        }
    }

    private fun createInt32Accessor(
        pos: Int, c: Int, given: AttributeLayout,
        result: StringBuilder, name: String, name1: CharSequence,
        stride4: Int, offset4: Int, setters: Boolean, type: String
    ) {
        val alignment = floatAlignment(c)
        if (pos % alignment != 0) {
            throw IllegalStateException("Alignment breaks std430: $given, $pos % $alignment")
        }
        result.appendGetterHeader(type, name, name1).appendIdx(stride4, offset4)
        result.append("return ").append(type).append('(')
        result.append(name).append(".data[idx]")
        if (c > 1) result.append(", ").append(name).append(".data[idx+1u]")
        if (c > 2) result.append(", ").append(name).append(".data[idx+2u]")
        if (c > 3) result.append(", ").append(name).append(".data[idx+3u]")
        result.append("); }\n")
        if (setters) {
            result.appendSetterHeader(type, name, name1).appendIdx(stride4, offset4)
            for (i in 0 until c) {
                result.append(name).append(".data[idx+").append(i).append("u] = uint(value")
                    .append(mask(c, i)).append(");\n")
            }
            result.append("}\n")
        }
    }

    fun createInt16Helper(builder: StringBuilder, name: String) {
        // todo only modify 2 bytes out of 4 ... could create concurrency issues!!!
        //  -> if we use these functions with setters, alignment always should >= 4

        // todo test these functions
        builder.append(
            "" +
                    "uint getU16$name(uint index) {\n" +
                    "   int shift = (index & 1) * 16;\n" +
                    "   return ($name.data[index>>1] >> shift) & 0xffffu;\n" +
                    "}\n" +
                    "void setU16$name(uint index, uint value) {\n" +
                    "   if ((index&1) == 0) {\n" + // set x, keep y
                    "       setU16$name(index,uvec2(value, getU16$name(index+1)));\n" +
                    "   } else {\n" + // set y, keep x
                    "       setU16$name(index-1,uvec2(getU16$name(index-1), value));\n" +
                    "   }\n" +
                    "}\n" +
                    "void setU16$name(uint index, uvec2 value) {\n" +
                    "   uint joined = (value.x << 16) | (value.y & 0xffffu);\n" +
                    "   $name.data[index>>1] = joined;\n" +
                    "}\n" +
                    "void setU16$name(uint index, uvec3 value) {\n" +
                    "   setU16$name(index, value.xy);\n" +
                    "   setU16$name(index+2, value.z);\n" +
                    "}\n" +
                    "void setU16$name(uint index, uvec4 value) {\n" +
                    "   setU16$name(index,   value.xy);\n" +
                    "   setU16$name(index+2, value.zw);\n" +
                    "}\n"
        )
    }

    private const val INV65535 = 1.0 / 65535.0
    private const val INV32767 = 1.0 / 32767.0
    private const val INV255 = 1.0 / 255.0
    private const val INV127 = 1.0 / 127.0
}