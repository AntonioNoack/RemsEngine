package me.anno.mesh.obj

import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import java.io.EOFException
import java.io.File
import java.io.InputStream
import kotlin.math.pow

open class OBJMTLReader(val reader: InputStream) {

    companion object {
        private val LOGGER = LogManager.getLogger(OBJMTLReader::class)
    }

    fun skipSpaces() {
        while (true) {
            when (val next = next()) {
                ' '.code, '\t'.code, '\r'.code, '\n'.code -> {
                }
                else -> {
                    putBack(next)
                    return
                }
            }
        }
    }

    fun skipLine() {
        while (true) {
            if (next() == '\n'.code) {
                // done :)
                return
            }
        }
    }

    var putBack = -1
    fun next(): Int {
        val char = if (putBack >= 0) putBack else reader.read()
        putBack = -1
        if (char == '\r'.code) return next()
        if (char < 0) throw EOFException()
        return char
    }

    fun nextChar(): Char = next().toChar()

    fun putBack(char: Int) {
        putBack = char
    }

    fun putBack(char: Char) {
        putBack = char.code
    }

    fun readIndex(): Int = readInt() - 1

    fun readInt(default: Int = 0): Int {
        var number = 0
        var isNegative = false
        var hadChar = false
        while (true) {
            when (val char = nextChar()) {
                '-' -> isNegative = true
                in '0'..'9' -> {
                    number = 10 * number + char.code - '0'.code
                    hadChar = true
                }
                else -> {
                    putBack(char)
                    return if (hadChar)
                        if (isNegative) -number
                        else number
                    else default
                }
            }
        }
    }

    fun readUntilSpace(): String {
        val builder = StringBuilder()
        while (true) {
            when (val char = next()) {
                ' '.code, '\t'.code, '\n'.code -> {
                    putBack(char)
                    return builder.toString()
                }
                else -> {
                    builder.append(char.toChar())
                }
            }
        }
    }

    // not perfect, but maybe faster
    // uses no allocations :)
    fun readFloat(): Float {
        var isNegative = false
        var number = 0f
        while (true) {
            when (val char = nextChar()) {
                '-' -> isNegative = true
                in '0'..'9' -> {
                    number = number * 10f + char.code - '0'.code
                }
                '.' -> {
                    var exponent = 0.1f
                    var fraction = 0f
                    while (true) {
                        when (val char2 = nextChar()) {
                            in '0'..'9' -> {
                                fraction = exponent * (char2.code - 48)
                                exponent *= 0.1f
                            }
                            'e', 'E' -> {
                                var exponentInt = 0
                                var expIsNegative = false
                                while (true) {
                                    when (val char3 = nextChar()) {
                                        '-' -> expIsNegative = true
                                        in '0'..'9' -> exponentInt = exponentInt * 10 + (char3.code - 48)
                                        else -> {
                                            putBack = char3.code
                                            val exp = if (expIsNegative) -exponentInt else +exponentInt
                                            return if (isNegative) {
                                                -(number + fraction) * 10f.pow(exp)
                                            } else {
                                                +(number + fraction) * 10f.pow(exp)
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                putBack = char2.code
                                return if (isNegative) {
                                    -(number + fraction)
                                } else {
                                    +(number + fraction)
                                }
                            }
                        }
                    }
                }
                'e' -> throw UnsupportedOperationException("exponent numbers not supported")
                else -> {
                    putBack = char.code
                    return if (isNegative) -number else +number
                }
            }
        }
    }

    fun readValue(): Float {
        skipSpaces()
        val x = readFloat()
        skipLine()
        return x
    }

    fun readVector2f(): Vector2f {
        skipSpaces()
        val x = readFloat()
        skipSpaces()
        val y = readFloat()
        skipLine()
        return Vector2f(x, y)
    }

    fun readVector3f(): Vector3f {
        skipSpaces()
        val x = readFloat()
        skipSpaces()
        val y = readFloat()
        skipSpaces()
        val z = readFloat()
        skipLine()
        return Vector3f(x, y, z)
    }

    fun readFile(parent: File): File {
        skipSpaces()
        val path = readUntilSpace()
        skipLine()
        val file = File(parent.parentFile, path)
        if (!file.exists()) LOGGER.warn("Missing file $file")
        return file
    }

    fun readFile(parent: FileReference): FileReference {
        skipSpaces()
        var path = readUntilSpace()
            .replace('\\', '/')
            .replace("//", "/")
        skipLine()
        if (path.startsWith("./")) path = path.substring(2)
        val file = getReference(parent.getParent(), path)
        if (!file.exists) LOGGER.warn("Missing file $file")
        return file
    }

}