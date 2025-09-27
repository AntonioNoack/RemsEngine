package me.anno.gpu.shader.builder

import me.anno.gpu.GFX.INVALID_POINTER
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.BufferUsage
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.GPUShader
import me.anno.utils.Color.toHexString
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL46C
import org.lwjgl.opengl.GL46C.GL_SHADER_STORAGE_BARRIER_BIT
import org.lwjgl.opengl.GL46C.glMemoryBarrier
import speiger.primitivecollections.UniqueValueIndexMap

object ShaderPrinting {

    private val LOGGER = LogManager.getLogger(ShaderPrinting::class)

    const val MAX_SIZE = 256
    const val SLOT = 32

    private const val BUFFER_SIZE = MAX_SIZE + 1

    const val PRINTING_LIB = "" +
            "#ifndef PRINTING_LIB\n" +
            "#define PRINTING_LIB\n" +

            "layout(std430, binding=$SLOT) buffer printBuffer1 { int[] printBuffer; };\n" +
            "int initPrinting(int size) {\n" +
            "   int id = atomicAdd(printBuffer[0], size);\n" +
            "   if(id >= 0 && id + size <= $MAX_SIZE) return 1 + id;\n" +
            "   else return -1000;\n" +
            "}\n" +
            "void push(int id, int value) { if(id >= 0) printBuffer[id] = value; }\n" +
            "void push(int id, uint value) { if(id >= 0) printBuffer[id] = int(value); }\n" +
            "void push(int id, float value) { if(id >= 0) printBuffer[id] = floatBitsToInt(value); }\n" +
            "#endif\n"

    fun definePrintCall(types: List<GLSLType>): String {
        val hash = types.hashCode().toHexString()
        val print = StringBuilder()
        print.append("#ifndef PRINT_").append(hash).append('\n')
            .append("#define PRINT_").append(hash).append('\n')
            .append("void println(int name")
        for (i in types.indices) {
            print.append(',').append(types[i].glslName).append(" v").append(i)
        }
        val numValues = types.sumOf { it.components }
        print.append(") { int id = initPrinting(")
            .append(numValues + 2).append(");push(id,name);push(id+1,")
            .append(numValues).append(");")
        var offset = 2
        for (i in types.indices) {
            val type = types[i]
            for (j in 0 until type.components) {
                print.append("push(id+").append(offset++).append(",v").append(i)
                if (type.components > 1) print.append('.').append('x' + j)
                print.append(");")
            }
        }
        print.append("}\n")
            .append("#endif\n")
        return print.toString()
    }

    val layout = bind(Attribute("value", AttributeType.SINT32, 1))

    val buffer = StaticBuffer(
        "println", layout, BUFFER_SIZE,
        BufferUsage.DYNAMIC
    )

    private const val PREFIX = "println(\""

    fun StringBuilder.implementPrinting(): Boolean {
        var startIndex = 0
        var changed = false
        while (true) {
            startIndex = indexOf(PREFIX, startIndex)
            if (startIndex < 0) break

            // find the end of the string
            var endIndex = startIndex + PREFIX.length
            while (endIndex < length) {
                when (this[endIndex++]) {
                    '"' -> break
                    '\\' -> endIndex++
                    else -> {}
                }
            }

            val format = substring(startIndex + PREFIX.length, endIndex - 1)
            val id = synchronized(printLookup) { printLookup.add(format) }
            replace(startIndex + PREFIX.length - 1, endIndex, (id + 1).toString())

            startIndex += PREFIX.length
            changed = true
        }
        return changed
    }

    fun bindPrintingBuffer(shader: GPUShader) {
        if (!shader.hasPrinting) return
        if (buffer.pointer == INVALID_POINTER) {
            buffer.target = GL46C.GL_SHADER_STORAGE_BUFFER
            val buffer = buffer.getOrCreateNioBuffer()
            buffer.position(BUFFER_SIZE * 4)
        }

        buffer.ensureBuffer()
        shader.bindBuffer(SLOT, buffer)
    }

    fun printFromBuffer() {
        if (printLookup.isEmpty()) return

        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT)

        val buffer = buffer.read()
        val usedSize = buffer.getInt(0)

        var ptr = 1
        while (ptr < usedSize && ptr < MAX_SIZE) {
            val nameId = buffer.getInt((ptr++) shl 2)
            val numParams = buffer.getInt((ptr++) shl 2)
            val name = printLookup.values.getOrNull(nameId - 1)
            if (name == null) {
                LOGGER.warn("Illegal name $nameId at ${ptr - 2}, #size: $usedSize")
                break
            }
            val args = formatInfo.getOrPut(name) { extractFormatInfo(name) }
            val formatted = name.format(*Array<Any?>(args.size) { idx ->
                if (idx < numParams && idx <= BUFFER_SIZE) {
                    val isFloat = args[idx]
                    val pos = (ptr + idx) shl 2
                    if (isFloat) buffer.getFloat(pos) else buffer.getInt(pos)
                } else Float.NaN
            })
            LOGGER.info(formatted)
            ptr += numParams
        }

        if (usedSize > MAX_SIZE) {
            LOGGER.warn("Too many println()-calls")
        }
        if (usedSize > 0) clear()
    }

    fun clear() {
        val bufferI = buffer.getOrCreateNioBuffer()
        bufferI.putInt(0, 0)
        bufferI.position(BUFFER_SIZE * 4)
        buffer.cpuSideChanged()
    }

    private fun extractFormatInfo(format: String): List<Boolean> {
        var i = 0
        val result = ArrayList<Boolean>()
        loop@ while (i < format.length) {
            when (format[i++]) {
                '%' -> {
                    while (i < format.length) {
                        when (format[i++]) {
                            'o', 'x', 'X', 'b', 'B', 'c', 'C', 'd' -> {
                                result.add(false) // not a float
                                continue@loop
                            }
                            'e', 'E', 'f', 'g', 'G', 'a', 'A' -> {
                                result.add(true) // a float
                                continue@loop
                            }
                        }
                    }
                }
                '\\' -> i++
                else -> {}
            }
        }
        println("$format -> $result")
        return result
    }

    private val printLookup = UniqueValueIndexMap<String>(-1)
    private val formatInfo = HashMap<String, List<Boolean>>()
}