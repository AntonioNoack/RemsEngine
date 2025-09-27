package me.anno.gpu.shader.builder

import me.anno.gpu.GFX
import me.anno.gpu.shader.GLSLType
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.withFlag
import kotlin.math.max

class Variable(
    val type: GLSLType, var name: String,
    var arraySize: Int, var inOutMode: VariableMode
) {

    // layout(rgba8, binding = 1) restrict coherent readonly writeonly uniform image2D dst;
    // layout(std430, binding=5) readonly buffer volumeBuffer1 { int volumeBuffer[]; };
    companion object {

        const val FLAG_FLAT = 1
        const val FLAG_RESTRICT = 2
        const val FLAG_READONLY = 4
        const val FLAG_WRITEONLY = 8
        const val FLAG_COHERENT = 16

        const val CHANNEL_OFFSET = 32 - 11
        const val CHANNEL_R = 0 shl CHANNEL_OFFSET
        const val CHANNEL_RG = 1 shl CHANNEL_OFFSET
        const val CHANNEL_RGB = 2 shl CHANNEL_OFFSET
        const val CHANNEL_RGBA = 3 shl CHANNEL_OFFSET

        const val NUMBER_OFFSET = 32 - 9
        const val NUMBER_FLOAT = 0 shl NUMBER_OFFSET
        const val NUMBER_INT = 1 shl NUMBER_OFFSET
        const val NUMBER_UINT = 2 shl NUMBER_OFFSET

        const val NUM_BITS_OFFSET = 32 - 7
        const val BITS_8 = 0 shl NUM_BITS_OFFSET
        const val BITS_16 = 1 shl NUM_BITS_OFFSET
        const val BITS_32 = 2 shl NUM_BITS_OFFSET
        const val BITS_64 = 3 shl NUM_BITS_OFFSET

        const val BINDING_OFFSET = 32 - 5

        fun appendLayoutAndFlags(str: StringBuilder, type: GLSLType, flags: Int) {
            str.append("layout(")
            val isImageType = type != GLSLType.BUFFER
            when (type) {
                GLSLType.BUFFER -> str.append("std430")
                GLSLType.IMAGE1D, GLSLType.IMAGE2D, GLSLType.IMAGE3D, GLSLType.IMAGE_CUBE -> {
                    val numChannels = 1 + (flags shr CHANNEL_OFFSET)
                    str.append("rgba", 0, numChannels)
                    val numBits = 8 shl (flags shr NUM_BITS_OFFSET)
                    str.append(numBits)
                    val format = when (flags and (3 shl NUMBER_OFFSET)) {
                        NUMBER_FLOAT -> "f"
                        NUMBER_INT -> "i"
                        else -> "ui"
                    }
                    str.append(format)
                }
                else -> throw IllegalArgumentException()
            }

            val binding = flags shr BINDING_OFFSET
            str.append(", binding=").append(binding)
            str.append(") ")

            if (flags.hasFlag(FLAG_RESTRICT)) str.append("restrict ")
            if (flags.hasFlag(FLAG_READONLY)) str.append("readonly ")
            if (flags.hasFlag(FLAG_WRITEONLY)) str.append("writeonly ")
            if (flags.hasFlag(FLAG_COHERENT)) str.append("coherent ")

            if (isImageType) {
                // special type-prefixes are needed for int-images
                when (flags and (3 shl NUMBER_OFFSET)) {
                    NUMBER_INT -> str.append("i")
                    NUMBER_UINT -> str.append("u")
                }
            }

            str.append(type.glslName)
        }
    }

    fun withType(inOutMode: VariableMode): Variable {
        if (inOutMode == this.inOutMode) return this
        return Variable(type, name, arraySize, inOutMode)
    }

    constructor(type: GLSLType, name: String, inOutMode: VariableMode) :
            this(type, name, -1, inOutMode)

    constructor(type: GLSLType, name: String, arraySize: Int, isIn: Boolean) :
            this(type, name, arraySize, if (isIn) VariableMode.IN else VariableMode.OUT)

    constructor(type: GLSLType, name: String, isIn: Boolean) :
            this(type, name, -1, if (isIn) VariableMode.IN else VariableMode.OUT)

    constructor(type: GLSLType, name: String, arraySize: Int) :
            this(type, name, arraySize, VariableMode.IN)

    constructor(components: Int, name: String) :
            this(GLSLType.floats[components - 1], name, VariableMode.IN)

    constructor(components: Int, name: String, inOutMode: VariableMode) :
            this(GLSLType.floats[components - 1], name, inOutMode)

    constructor(components: Int, name: String, arraySize: Int = -1) :
            this(GLSLType.floats[components - 1], name, arraySize)

    constructor(type: GLSLType, name: String) :
            this(type, name, -1, VariableMode.IN)

    constructor(base: Variable, mode: VariableMode) :
            this(base.type, base.name, base.arraySize, mode)

    fun defineBufferFormat(structTypeName: String, isArray: Boolean): Variable {
        TODO()
        return this
    }

    fun defineImageFormat(numChannels: Int, numberType: ImageNumberType, numBits: Int): Variable {
        return defineImageFormatByEnums(
            (numChannels - 1) shl CHANNEL_OFFSET,
            numberType.id,
            when (numBits) {
                8 -> BITS_8
                16 -> BITS_16
                32 -> BITS_32
                64 -> BITS_64
                else -> throw IllegalArgumentException("Unsupported number of bits")
            }
        )
    }

    fun binding(i: Int): Variable {
        this.binding = i
        return this
    }

    fun defineImageFormatByEnums(channels: Int, numberType: Int, numBits: Int): Variable {
        val channelMask = 3 shl CHANNEL_OFFSET
        val numberMask = 3 shl NUMBER_OFFSET
        val bitsMask = 3 shl NUM_BITS_OFFSET
        assertEquals(0, channels and channelMask.inv())
        assertEquals(0, numberType and numberMask.inv())
        assertEquals(0, numBits and numberMask.inv())
        val joinedMask = channelMask or numberMask or bitsMask
        flags = (flags and joinedMask.inv()) or (channels or numberType or numBits)
        return this
    }

    fun flat(): Variable {
        isFlat = true
        return this
    }

    val size
        get() = when (type) {
            GLSLType.V1B -> 5
            GLSLType.V1I -> 7
            GLSLType.V1F -> 10
            GLSLType.V2B, GLSLType.V2I, GLSLType.V2F -> 20
            GLSLType.V3B, GLSLType.V3I, GLSLType.V3F -> 30
            GLSLType.V4B, GLSLType.V4I, GLSLType.V4F -> 40
            GLSLType.M2x2 -> 40
            GLSLType.M3x3 -> 90
            GLSLType.M4x3 -> 120
            GLSLType.M4x4 -> 160
            else -> 1000
        } * max(1, arraySize)

    fun declare(code: StringBuilder, prefix: String?, assign: Boolean) {
        if (prefix != null && prefix.startsWith("uniform") && arraySize > 0 && type.isSampler) {
            // define sampler array
            val type = if (!GFX.supportsDepthTextures) type.withoutShadow() else type
            for (index in 0 until arraySize) {
                code.append(prefix).append(' ')
                code.append(type.glslName).append(' ')
                code.append(name).append(index).append(";\n")
            }
        } else {
            // define normal variable
            if (prefix != null) code.append(prefix).append(' ')
            code.append(type.glslName)
            if (isArray) {
                code.append('[').append(arraySize).append(']')
            }
            code.append(' ').append(name)
            if (assign) {
                when (type) {
                    GLSLType.V1B -> code.append("=false;\n")
                    GLSLType.V1F -> code.append("=0.0;\n")
                    GLSLType.V2F -> code.append("=vec2(0.0,0.0);\n")
                    GLSLType.V3F -> code.append("=vec3(0.0,0.0,0.0);\n")
                    GLSLType.V4F -> code.append("=vec4(0.0,0.0,0.0,0.0);\n")
                    GLSLType.V1I -> code.append("=0;\n")
                    GLSLType.V2I -> code.append("=ivec2(0,0);\n")
                    GLSLType.V3I -> code.append("=ivec3(0,0,0);\n")
                    GLSLType.V4I -> code.append("=ivec4(0,0,0,0);\n")
                    else -> code.append(";\n")
                }
            } else code.append(";\n")
        }
    }

    fun declare0(code: StringBuilder, prefix: String? = null) {
        if (prefix != null && prefix.startsWith("uniform") && arraySize > 0 && type.isSampler) {
            throw IllegalStateException("Cannot assign to uniform array")
        } else {
            // define normal variable
            if (prefix != null) code.append(prefix).append(' ')
            code.append(type.glslName)
            if (arraySize >= 0) {
                code.append('[').append(arraySize).append(']')
            }
            code.append(' ').append(name)
        }
    }

    var slot = -1
    var flags = 0

    var isFlat: Boolean
        get() = flags.hasFlag(FLAG_FLAT)
        set(value) {
            flags = flags.withFlag(FLAG_FLAT, value)
        }

    /**
     * Only defined for Images and Buffers, not for textures!
     * (Compute Shaders Only!)
     * */
    var binding: Int
        get() = flags ushr BINDING_OFFSET
        set(value) {
            val mask = 31.shl(BINDING_OFFSET)
            flags = (flags and mask.inv()) or (value shl BINDING_OFFSET)
        }

    /**
     * Only defined for Image1D, Image2D, Image3D and ImageCube!
     * (Compute Shaders Only!)
     * */
    val numImageChannels get() = 1 + (flags shr CHANNEL_OFFSET)

    override fun equals(other: Any?): Boolean {
        return other is Variable && other.type == type && other.name == name
    }

    override fun hashCode(): Int {
        return type.hashCode() * 31 + name.hashCode()
    }

    override fun toString(): String {
        return "${if (isFlat) "flat " else ""}${inOutMode.glslName} ${type.glslName} $name"
    }

    val isAttribute get() = inOutMode == VariableMode.ATTR
    val isInput get() = inOutMode != VariableMode.OUT
    val isOutput get() = inOutMode == VariableMode.OUT || inOutMode == VariableMode.INOUT
    val isModified get() = inOutMode == VariableMode.OUT || inOutMode == VariableMode.INOUT || inOutMode == VariableMode.INMOD
    val isArray get() = arraySize >= 0
}
