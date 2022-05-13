package me.anno.mesh.obj

import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import java.io.EOFException
import java.io.InputStream
import kotlin.math.pow

open class OBJMTLReader(val reader: InputStream) {

    companion object {
        private val LOGGER = LogManager.getLogger(OBJMTLReader::class)
        private const val minus = '-'.code
        private const val zero = '0'.code
        private const val nine = '9'.code
        private const val dot = '.'.code
        private const val smallE = 'e'.code
        private const val largeE = 'E'.code
        private const val space = ' '.code
        private const val tab = '\t'.code
        private const val newLine = '\n'.code
        private const val newLine2 = '\r'.code
    }

    fun skipSpaces(skipNewLine: Boolean = false) {
        while (true) {
            when (val next = next()) {
                space, tab, newLine2 -> {
                }
                newLine -> {
                    if (!skipNewLine) {
                        putBack(next)
                        return
                    }
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
            if (next() == newLine) {
                // done :)
                return
            }
        }
    }

    var putBack = -1
    fun next(): Int {
        val char = if (putBack >= 0) putBack else reader.read()
        putBack = -1
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

    fun readIndex(numVertices: Int): Int {
        val v = readInt()
        return if (v < 0) {
            // indices from the end of the file
            numVertices + v
        } else v - 1 // indices, starting at 1
    }

    fun readInt(default: Int = 0): Int {
        var number = 0
        var sign = +1
        when (val char = next()) {
            minus -> sign = -1
            in zero..nine -> {
                number = char - 48
            }
            else -> {
                putBack(char)
                return default
            }
        }
        val reader = reader
        while (true) {
            val char = reader.read()
            if (char in zero..nine) {
                number = 10 * number + char - 48
            } else {
                putBack(char)
                return sign * number
            }
        }
    }

    fun readUntilSpace(): String {
        val builder = StringBuilder()
        while (true) {
            when (val char = next()) {
                newLine2 -> {}
                space, tab, newLine -> {
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
        var sign = 1f
        var number = 0
        val reader = reader
        while (true) {
            when (val char = next()) {
                minus -> sign = -sign
                in zero..nine -> {
                    number = number * 10 + char - 48
                }
                dot -> {
                    var exponent = 0.1f
                    var fraction = 0f
                    while (true) {
                        when (val char2 = reader.read()) {
                            in zero..nine -> {
                                fraction += exponent * (char2 - 48)
                                exponent *= 0.1f
                            }
                            smallE, largeE -> {
                                val power = readInt()
                                return sign * (number + fraction) * 10f.pow(power)
                            }
                            else -> {
                                putBack = char2
                                return sign * (number + fraction)
                            }
                        }
                    }
                }
                smallE, largeE -> {
                    val power = readInt()
                    return sign * number * 10f.pow(power)
                }
                else -> {
                    putBack = char
                    return sign * number.toFloat()
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

    @Suppress("unused")
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