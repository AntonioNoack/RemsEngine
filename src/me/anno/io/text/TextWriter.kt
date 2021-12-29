package me.anno.io.text

import me.anno.io.BufferedIO.useBuffered
import me.anno.io.ISaveable
import me.anno.io.files.FileReference

class TextWriter(initialCapacity: Int) : TextWriterBase() {

    constructor() : this(32)

    private val data = StringBuilder(initialCapacity)

    /** you should not use this function
     * if you use it, your file no longer will be readable (probably)
     * */
    fun getFoolishWriteAccess() = data

    override fun append(v: Char) {
        data.append(v)
    }

    override fun append(v: Int) {
        data.append(v)
    }

    override fun append(v: Long) {
        data.append(v)
    }

    override fun append(v: String) {
        data.append(v)
    }

    /**
     * returns the result as a string;
     * only call it, if all writing operations have finished!
     * */
    override fun toString(): String = data.toString()

    companion object {

        fun toText(data: List<ISaveable>): String {
            val writer = TextWriter()
            for (entry in data) writer.add(entry)
            writer.writeAllInList()
            return writer.toString()
        }

        fun toText(entry: ISaveable): String {
            val writer = TextWriter()
            writer.add(entry)
            writer.writeAllInList()
            return writer.toString()
        }

        fun save(entry: ISaveable, file: FileReference) {
            file.outputStream().useBuffered().use {
                val writer = TextStreamWriter(it)
                writer.add(entry)
                writer.writeAllInList()
            }
        }

        fun save(data: List<ISaveable>, file: FileReference) {
            file.outputStream().useBuffered().use {
                val writer = TextStreamWriter(it)
                for (entry in data) writer.add(entry)
                writer.writeAllInList()
            }
        }

        fun toBuilder(data: ISaveable): StringBuilder {
            val writer = TextWriter()
            writer.add(data)
            writer.writeAllInList()
            return writer.data
        }

    }


}