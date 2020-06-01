package me.anno.io.text

import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.Saveable
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class TextWriter(val beautify: Boolean): BaseWriter() {

    val separator = if(beautify) ", " else ","

    var data = StringBuilder()
    var hasObject = false

    operator fun StringBuilder.plusAssign(char: Char){
        append(char)
    }

    operator fun StringBuilder.plusAssign(text: String){
        append(text)
    }

    fun open(array: Boolean){
        data += if(array) '[' else '{'
        hasObject = false
    }

    fun close(array: Boolean){
        data += if(array) ']' else '}'
        hasObject = true
    }

    fun next(){
        if(hasObject){
            data += ','
        }
        hasObject = true
    }

    fun writeEscaped(value: String){
        var i = 0
        var lastI = 0
        fun put(){
            if(i > lastI){
                data += value.substring(lastI, i)
            }
            lastI = i+1
        }
        while(i < value.length){
            when(value[i]){
                '\\' -> {
                    put()
                    data += "\\\\"
                }
                '\t' -> {
                    put()
                    data += "\\t"
                }
                '\r' -> {
                    put()
                    data += "\\r"
                }
                '\n' -> {
                    put()
                    data += "\\n"
                }
                '"' -> {
                    put()
                    data += "\\\""
                }
                '\b' -> {
                    put()
                    data += "\\b"
                }
                12.toChar() -> {
                    put()
                    data += "\\f"
                }
                else -> {} // nothing
            }
            i++
        }
        put()
    }

    fun writeString(value: String){
        data += '"'
        writeEscaped(value)
        data += '"'
    }

    fun writeTypeNameString(type: String, name: String?){
        if(name != null){
            data += '"'
            writeEscaped(type)
            data += ':'
            writeEscaped(name)
            data += '"'
        }
    }

    fun writeAttributeStart(type: String, name: String?){
        if(name != null){
            next()
            writeTypeNameString(type, name)
            data += ':'
        }
    }

    override fun writeBool(name: String, value: Boolean, force: Boolean) {
        if(force || value){
            writeAttributeStart("b", name)
            data += if(value) "true" else "false"
        }
    }

    override fun writeByte(name: String, value: Byte, force: Boolean) {
        if(force || value != 0.toByte()){
            writeAttributeStart("B", name)
            data += value.toString()
        }
    }

    override fun writeShort(name: String, value: Short, force: Boolean) {
        if(force || value != 0.toShort()){
            writeAttributeStart("s", name)
            data += value.toString()
        }
    }

    override fun writeInt(name: String, value: Int, force: Boolean) {
        if(force || value != 0){
            writeAttributeStart("i", name)
            data += value.toString()
        }
    }

    override fun writeIntArray(name: String, value: IntArray, force: Boolean) {
        TODO("Not yet implemented")
    }

    override fun writeFloat(name: String, value: Float, force: Boolean) {
        if(force || value != 0f){
            writeAttributeStart("f", name)
            data += value.toString()
        }
    }

    override fun writeDouble(name: String, value: Double, force: Boolean) {
        if(force || value != 0.0){
            writeAttributeStart("d", name)
            data += value.toString()
        }
    }

    override fun writeString(name: String, value: String?, force: Boolean) {
        if(force || (value != null && value != "")){
            writeAttributeStart("S", name)
            if(value == null) data += "null"
            else writeString(value)
        }
    }

    override fun writeLong(name: String, value: Long, force: Boolean) {
        if(force || value != 0L){
            writeAttributeStart("l", name)
            data += value.toString()
        }
    }

    override fun writeVector2(name: String, value: Vector2f, force: Boolean) {
        if(force || value.x != 0f || value.y != 0f){
            writeAttributeStart("v2", name)
            data += '['
            data += value.x.toString()
            data += separator
            data += value.y.toString()
            data += ']'
        }
    }

    override fun writeVector3(name: String, value: Vector3f, force: Boolean) {
        if(force || value.x != 0f || value.y != 0f || value.z != 0f){
            writeAttributeStart("v3", name)
            data += '['
            data += value.x.toString()
            data += separator
            data += value.y.toString()
            data += separator
            data += value.z.toString()
            data += ']'
        }
    }

    override fun writeVector4(name: String, value: Vector4f, force: Boolean) {
        if(force || value.x != 0f || value.y != 0f || value.z != 0f || value.w != 0f){
            writeAttributeStart("v4", name)
            data += '['
            data += value.x.toString()
            data += separator
            data += value.y.toString()
            data += separator
            data += value.z.toString()
            data += separator
            data += value.w.toString()
            data += ']'
        }
    }

    override fun writeNull(name: String?) {
        writeAttributeStart("?", name)
        data += "null"
    }

    override fun writeListStart() {
        open(true)
    }

    override fun writeListSeparator() {
        data += ','
        hasObject = true
    }

    override fun writeListEnd() {
        close(true)
    }

    override fun <V : Saveable> writeList(self: ISaveable?, name: String, elements: List<V>?, force: Boolean) {
        elements?.forEach {
            writeObject(self, name, it, force)
        }
    }

    override fun writeListV2(name: String, elements: List<Vector2f>?, force: Boolean) {
        if(elements != null){
            writeAttributeStart("v2[]", name)
            open(true)
            elements.forEach {
                data += '['
                data += it.x.toString()
                data += separator
                data += it.y.toString()
                data += ']'
            }
            close(true)
        }
    }

    override fun writeListV3(name: String, elements: List<Vector3f>?, force: Boolean) {
        if(elements != null){
            writeAttributeStart("v2[]", name)
            open(true)
            elements.forEach {
                data += '['
                data += it.x.toString()
                data += separator
                data += it.y.toString()
                data += separator
                data += it.z.toString()
                data += ']'
            }
            close(true)
        }
    }

    override fun writeListV4(name: String, elements: List<Vector4f>?, force: Boolean) {
        if(elements != null){
            writeAttributeStart("v2[]", name)
            open(true)
            elements.forEach {
                data += '['
                data += it.x.toString()
                data += separator
                data += it.y.toString()
                data += separator
                data += it.z.toString()
                data += separator
                data += it.w.toString()
                data += ']'
            }
            close(true)
        }
    }

    override fun writeObjectImpl(name: String?, value: ISaveable) {
        if(name != null && name.isNotEmpty()){
            writeAttributeStart(value.getClassName(), name)
            open(false)
        } else {
            open(false)
            writeString("class")
            data += ':'
            writeString(value.getClassName())
            hasObject = true
        }
        writeInt("*ptr", pointers[value]!!)
        value.save(this)
        close(false)
    }

    override fun writePointer(name: String?, className: String, ptr: Int) {
        writeAttributeStart(className, name)
        data += ptr.toString()
    }

    override fun toString(): String = data.toString()

    companion object {

        fun toText(data: List<Saveable>, beautify: Boolean): String {
            val writer = TextWriter(beautify)
            for(entry in data) writer.add(entry)
            writer.writeAllInList()
            return writer.toString()
        }

        fun toText(data: Saveable, beautify: Boolean): String {
            val writer = TextWriter(beautify)
            writer.add(data)
            writer.writeAllInList()
            return writer.toString()
        }

    }



}