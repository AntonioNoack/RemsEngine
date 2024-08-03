package me.anno.gpu.buffer

import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.MainStage.Companion.instBufferName
import me.anno.gpu.shader.builder.MainStage.Companion.meshBufferName
import me.anno.gpu.shader.builder.Variable
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt

object AttributeLoading {

    fun appendAttributeLoader(
        code: StringBuilder, main: StringBuilder,
        attr: Attribute, variable: Variable,
        isInstanced: Boolean
    ) {
        assertFalse(variable.type.isSampler)
        // declare variable, so it can be used in the whole shader
        code.append(variable.type.glslName)
        code.append(" ").append(variable.name)
        code.append(";\n")

        appendAttributeLoader2(main, attr, variable, isInstanced)
    }

    private fun appendAttributeLoader2(
        main: StringBuilder,
        attr: Attribute, variable: Variable,
        isInstanced: Boolean
    ) {

        // load variable from respective buffer
        main.append(variable.name).append(" = ")

        val bufferName = if (isInstanced) instBufferName else meshBufferName
        val idName = if (isInstanced) "gl_InstanceID" else "gl_VertexID"

        val isGPULittleEndian = true // we'd need to find this out
        fun loadIntS32(component: Int) {
            // todo this might break with fractional overlap
            // validate stride
            assertEquals(0, attr.stride.and(3))
            main.append(bufferName)
            main.append('[').append(idName)
                .append(" * ").append(attr.stride.shr(2))
                .append(" + ").append(attr.offset.shr(2) + component)
                .append(']')
        }

        fun loadIntS(component: Int, shl: Int, shr: Int) {
            main.append("(")
            loadIntS32(component.shr(1))
            if (shl != 0) {
                main.append("<<").append(shl)
            }
            main.append(")>>").append(shr)
        }

        fun loadIntU(component: Int, shl: Int, mask: Int) {
            main.append("uint((")
            loadIntS32(component.shr(1))
            if (shl != 0) {
                main.append("<<").append(shl)
            }
            main.append(")&").append(mask).append(')')
        }

        fun loadInt(component: Int, type: AttributeType) {
            when (type) {
                AttributeType.SINT32 -> {
                    loadIntS32(component)
                }
                AttributeType.UINT32 -> {
                    main.append("uint(")
                    loadIntS32(component)
                    main.append(")")
                }
                AttributeType.SINT16 -> {
                    val shl = (component.hasFlag(1) != isGPULittleEndian).toInt(16)
                    loadIntS(component.shr(1), shl, 16)
                }
                AttributeType.UINT16 -> {
                    val shl = (component.hasFlag(1) == isGPULittleEndian).toInt(16)
                    loadIntU(component.shr(1), shl, 0xffff)
                }
                AttributeType.SINT8 -> {
                    var shl = component.and(3) * 8
                    if (isGPULittleEndian) shl = 24 - shl
                    loadIntS(component.shr(2), shl, 24)
                }
                AttributeType.UINT8 -> {
                    var shl = component.and(3) * 8
                    if (!isGPULittleEndian) shl = 24 - shl
                    loadIntU(component.shr(2), shl, 0xff)
                }
                else -> throw NotImplementedError("loadInt($type)")
            }
        }

        fun loadInt(component: Int) {
            loadInt(component, attr.type)
        }

        fun loadFloat(component: Int) {
            when (attr.type) {
                AttributeType.FLOAT -> {
                    main.append("intBitsToFloat(")
                    loadInt(component, AttributeType.SINT32)
                    main.append(")")
                }
                AttributeType.SINT8_NORM -> loadInt(component, AttributeType.SINT8)
                AttributeType.SINT16_NORM -> loadInt(component, AttributeType.SINT16)
                AttributeType.SINT32_NORM -> loadInt(component, AttributeType.SINT32)
                AttributeType.UINT8_NORM -> loadInt(component, AttributeType.UINT8)
                AttributeType.UINT16_NORM -> loadInt(component, AttributeType.UINT16)
                AttributeType.UINT32_NORM -> loadInt(component, AttributeType.UINT32)
                AttributeType.SINT8, AttributeType.SINT16, AttributeType.SINT32,
                AttributeType.UINT8, AttributeType.UINT16, AttributeType.UINT32 -> {
                    main.append("float(")
                    loadInt(component, attr.type)
                    main.append(")")
                }
                else -> throw NotImplementedError("loadFloat(${attr.type})")
            }
        }

        fun loadFloatFinish() {
            val bitCount = when (attr.type) {
                AttributeType.SINT8_NORM -> 7
                AttributeType.SINT16_NORM -> 15
                AttributeType.SINT32_NORM -> 31
                AttributeType.UINT8_NORM -> 8
                AttributeType.UINT16_NORM -> 16
                AttributeType.UINT32_NORM -> 32
                else -> return
            }
            main.append("*").append(1.0 / (1L.shl(bitCount) - 1))
        }

        when (val type = variable.type) {
            GLSLType.V1F -> {
                loadFloat(0)
                loadFloatFinish()
            }
            GLSLType.V1I -> {
                main.append(if (attr.type.signed) "(" else "int(")
                loadInt(0)
                main.append(")")
            }
            GLSLType.V2I, GLSLType.V3I, GLSLType.V4I -> {
                val prefix = if (attr.type.signed) "((" else "(int("
                val separator = if (attr.type.signed) "),(" else "),int("
                main.append(type.glslName).append(prefix)
                for (i in 0 until type.components) {
                    if (i > 0) main.append(separator)
                    loadInt(i)
                }
                main.append("))")
            }
            GLSLType.V1B -> {
                loadInt(0)
                main.append(if (attr.type.signed) "!=0" else "!=0u")
            }
            GLSLType.V2B, GLSLType.V3B, GLSLType.V4B -> {
                val check = if (attr.type.signed) "!=0" else "!=0u"
                main.append(type.glslName).append('(')
                for (i in 0 until type.components) {
                    if (i > 0) main.append(',')
                    loadInt(i)
                    main.append(check)
                }
                main.append(")")
            }
            else -> {
                // it's a float vector or a float matrix
                main.append(type.glslName).append('(')
                for (i in 0 until type.components) {
                    if (i > 0) main.append(',')
                    loadFloat(i)
                }
                main.append(")")
                loadFloatFinish()
            }
        }
        main.append(";\n")
    }

    fun appendAttributeZero(code: StringBuilder, variable: Variable) {
        assertFalse(variable.type.isSampler)
        code.append("const ").append(variable.type.glslName)
        code.append(" ").append(variable.name).append(" = ")
        when (variable.type) {
            GLSLType.V1B -> code.append("false")
            GLSLType.V1I -> code.append("0")
            GLSLType.V1F -> code.append("0.0")
            else -> code.append(variable.type).append("(0)") // meh
        }
        code.append(";\n")
    }
}