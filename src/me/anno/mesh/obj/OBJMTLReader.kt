package me.anno.mesh.obj

import me.anno.io.FileReference
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import java.io.EOFException
import java.io.File
import java.io.InputStream
import java.lang.StringBuilder

open class OBJMTLReader(val reader: InputStream){

    companion object {
        private val LOGGER = LogManager.getLogger(OBJMTLReader::class)
    }

    fun skipSpaces(){
        while(true){
            when(val next = next()){
                ' '.toInt(), '\t'.toInt(), '\r'.toInt(), '\n'.toInt() -> { }
                else -> {
                    putBack(next)
                    return
                }
            }
        }
    }

    fun skipLine(){
        while(true){
            if(next() == '\n'.toInt()){
                // done :)
                return
            }
        }
    }

    var putBack = -1
    fun next(): Int {
        val char = if(putBack >= 0) putBack else reader.read()
        putBack = -1
        if(char == '\r'.toInt()) return next()
        if(char < 0) throw EOFException()
        return char
    }

    fun putBack(char: Int){
        putBack = char
    }

    fun readUntilSpace(): String {
        val builder = StringBuilder()
        while(true){
            when(val char = next()){
                ' '.toInt(), '\t'.toInt(), '\n'.toInt() -> {
                    putBack(char)
                    return builder.toString()
                }
                else -> {
                    builder.append(char.toChar())
                }
            }
        }
    }

    fun readFloat() = readUntilSpace().toFloat()

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
        if(!file.exists()) LOGGER.warn("Missing file $file")
        return file
    }

    fun readFile(parent: FileReference): FileReference {
        skipSpaces()
        val path = readUntilSpace()
        skipLine()
        val file = File(parent.file.parentFile, path)
        if(!file.exists()) LOGGER.warn("Missing file $file")
        return FileReference(file)
    }

}