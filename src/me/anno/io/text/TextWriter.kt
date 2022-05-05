package me.anno.io.text

import me.anno.io.BufferedIO.useBuffered
import me.anno.io.ISaveable
import me.anno.io.files.FileReference

class TextWriter(initialCapacity: Int, workspace: FileReference) : TextWriterBase(workspace) {

    constructor(workspace: FileReference) : this(32, workspace)

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

        fun toText(data: Collection<ISaveable>, workspace: FileReference): String {
            val writer = TextWriter(workspace)
            for (entry in data) writer.add(entry)
            writer.writeAllInList()
            return writer.toString()
        }

        fun toText(entry: ISaveable, workspace: FileReference): String {
            val writer = TextWriter(workspace)
            writer.add(entry)
            writer.writeAllInList()
            return writer.toString()
        }

        fun save(entry: ISaveable, file: FileReference, workspace: FileReference) {
            file.outputStream().useBuffered().use {
                val writer = TextStreamWriter(it, workspace)
                writer.add(entry)
                writer.writeAllInList()
            }
        }

        fun save(data: Collection<ISaveable>, file: FileReference, workspace: FileReference) {
            file.outputStream().useBuffered().use {
                val writer = TextStreamWriter(it, workspace)
                for (entry in data) writer.add(entry)
                writer.writeAllInList()
            }
        }

        fun toBuilder(data: ISaveable, workspace: FileReference): StringBuilder {
            val writer = TextWriter(workspace)
            writer.add(data)
            writer.writeAllInList()
            return writer.data
        }

        /* a test, because smileys were not written correctly
        @JvmStatic
        fun main(args: Array<String>) {
            // smileys were not saved correctly, why?
            // because the input stream reader was reading bytes, not chars
            RemsRegistry.init()
            val smiley = "🤔"
            val text = Text(smiley)
            text.text.set(smiley)
            val asString = toText(text)
            LOGGER.info(asString)
            val asText = TextReader.read(asString).first() as Text
            LOGGER.info("Decoded text: ${asText.text[0.0]}, ${asText.text[0.0].length}")
            // this works so far...
            val tmp = FileFileRef.createTempFile("smiley", ".tmp")
            val ref = getReference(tmp.absolutePath)
            ref.writeText(asString)
            LOGGER.info(ref.readText())
            LOGGER.info(tmp)
            // tmp.delete()
        }*/

    }


}