package me.anno.ecs.prefab.change

import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path.Companion.ROOT_PATH
import me.anno.io.ISaveable
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import org.apache.logging.log4j.LogManager
import java.text.ParseException

// todo how do we reference (as variables) to other Entities? probably a path would be correct...
// todo the same for components

abstract class Change(val priority: Int) : Saveable(), Cloneable {

    var path: Path = ROOT_PATH

    abstract fun withPath(path: Path): Change

    fun apply(instance0: PrefabSaveable, chain: MutableSet<FileReference>?) {
        val instance = Hierarchy.getInstanceAt(instance0, path) ?: return
        applyChange(instance, chain)
    }

    abstract fun applyChange(instance: PrefabSaveable, chain: MutableSet<FileReference>?)

    /**
     * shallow copy
     * */
    public abstract override fun clone(): Change

    override fun save(writer: BaseWriter) {
        super.save(writer)
        val path = path
        // todo writing as object somehow introduces errors...
        writer.writeObject(null, "path", path)
        /*if (!path.isEmpty()) {
            writer.writeString("path", path.toString(), true)
        }*/
    }

    override fun readObject(name: String, value: ISaveable?) {
        if (name == "path" && value is Path) {
            path = value
        } else super.readObject(name, value)
    }

    override fun readString(name: String, value: String?) {
        if (name == "path") {
            try {
                path = Path.parse(value)
            } catch (e: ParseException) {
                super.readString(name, value)
            }
        } else super.readString(name, value)
    }

    companion object {

        private val LOGGER = LogManager.getLogger(Change::class)

        @JvmStatic
        fun main(args: Array<String>) {
            registerCustomClass(CAdd())
            registerCustomClass(CSet())
            registerCustomClass(Path())
            val path = Path(arrayOf("k", "l", "m"), intArrayOf(4, 5, 6), charArrayOf('x', 'y', 'z'))
            for (sample in listOf(
                CSet(path, "path", "z"),
                CAdd(path, 'x', "Entity")
            )) {
                LOGGER.info(sample)
                val clone = TextReader.read(TextWriter.toText(sample)).first()
                LOGGER.info(clone)
            }
        }

    }

}
