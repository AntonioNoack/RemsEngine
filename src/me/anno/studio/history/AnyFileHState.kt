package me.anno.studio.history

import me.anno.io.base.BaseWriter
import java.io.File

class AnyFileHState(file: File): HistoryState<ByteArray?>(){

    init {
        setFile(file)
    }

    fun setFile(file: File){
        state = if(file.exists()) file.readBytes() else null
        this.file = file
    }

    private var file: File? = null

    override fun apply(state: ByteArray?) {
        val file = file ?: return
        if(state != null){
            file.parentFile?.mkdirs()
            file.writeBytes(state)
        } else {
            file.delete()
        }
    }

    override fun writeState(writer: BaseWriter, name: String, v: ByteArray?) {
        // todo encode somehow...
        // writer.writeString(name, v)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("file", file.toString())
    }

    // todo decode somehow
    override fun readString(name: String, value: String) {
        when(name){
            "file" -> file = File(value)
            else -> super.readString(name, value)
        }
    }

    override fun getClassName(): String = "TextFileHState"

}