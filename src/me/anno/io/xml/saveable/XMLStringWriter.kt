package me.anno.io.xml.saveable

import me.anno.io.ISaveable
import me.anno.io.files.FileReference

/**
 * todo we currently save data as JSON, but it might be nice to also be able to save it as XML and YAML
 * */
class XMLStringWriter(initialCapacity: Int, workspace: FileReference) : XMLWriterBase(workspace) {

    constructor(workspace: FileReference) : this(32, workspace)

    private val data = StringBuilder(initialCapacity)

    /** you should not use this function
     * if you use it, your file no longer will be readable (probably)
     * */
    @Suppress("unused")
    fun getFoolishWriteAccess() = data

    override fun append(v: String) {
        data.append(v)
    }

    /**
     * returns the result as a string;
     * only call it, if all writing operations have finished!
     * */
    override fun toString(): String = data.toString()

    companion object {

        fun toText(data: Collection<ISaveable>, workspace: FileReference): String {
            val writer = XMLStringWriter(workspace)
            for (entry in data) writer.add(entry)
            writer.writeAllInList()
            return writer.toString()
        }

        fun toText(entry: ISaveable, workspace: FileReference): String {
            val writer = XMLStringWriter(workspace)
            writer.add(entry)
            writer.writeAllInList()
            return writer.toString()
        }

        @Suppress("unused")
        fun toBuilder(data: ISaveable, workspace: FileReference): StringBuilder {
            val writer = XMLStringWriter(workspace)
            writer.add(data)
            writer.writeAllInList()
            return writer.data
        }
    }
}