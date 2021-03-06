package me.anno.io.json

import me.anno.utils.types.Strings
import java.io.OutputStream

/**
 * simple class to write JSON content; if you write Saveables, consider BinaryWriter and TextWriter first!
 * used in Rem's Studio to save UI layout
 * */
@Suppress("unused")
class JsonWriter(val output: OutputStream) {

    private var first = true
    private var isKeyValue = false

    private fun writeString(value: String){
        output.write('"'.code)
        val sb = StringBuilder()
        Strings.writeEscaped(value, sb)
        output.write(sb.toString().toByteArray())
        output.write('"'.code)
    }

    fun keyValue(key: String) {
        next()
        writeString(key)
        isKeyValue = true
    }

    private fun next(){
        if(!first){
            output.write(if (isKeyValue) ':'.code else ','.code)
        }
        isKeyValue = false
        first = false
    }

    fun write(b: Boolean){
        next()
        if(b){
            output.write("true".toByteArray())
        } else {
            output.write("false".toByteArray())
        }
    }

    fun write(i: Int){
        next()
        output.write(i.toString().toByteArray())
    }

    fun write(l: Long){
        next()
        output.write(l.toString().toByteArray())
    }

    fun write(f: Float){
        next()
        output.write(f.toString().toByteArray())
    }

    fun write(d: Double){
        next()
        output.write(d.toString().toByteArray())
    }

    fun write(value: String){
        next()
        writeString(value)
    }

    fun open(array: Boolean) {
        next()
        output.write(if (array) '['.code else '{'.code)
        first = true
    }

    fun close(array: Boolean) {
        output.write(if (array) ']'.code else '}'.code)
        first = false
    }

}