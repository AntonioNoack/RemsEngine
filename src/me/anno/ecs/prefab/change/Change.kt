package me.anno.ecs.prefab.change

import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path.Companion.ROOT_PATH
import me.anno.io.ISaveable
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import org.apache.logging.log4j.LogManager
import java.text.ParseException

abstract class Change : Saveable(), Cloneable {

    var path: Path = ROOT_PATH

    fun apply(instance0: PrefabSaveable, depth: Int) {
        if (instance0.prefabPath != ROOT_PATH) throw RuntimeException("Root instance must have root path, got ${instance0.prefabPath}")
        val instance = Hierarchy.getInstanceAt(instance0, path) ?: return
        if (instance.prefabPath != path) throw RuntimeException("Path does not match! ${instance.prefabPath} != $path")
        applyChange(instance, depth)
    }

    abstract fun applyChange(instance: PrefabSaveable, depth: Int)

    /**
     * shallow copy
     * */
    public abstract override fun clone(): Change

    override fun save(writer: BaseWriter) {
        super.save(writer)
        val path = path
        writer.writeObject(null, "path", path)
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
            registerCustomClass(ROOT_PATH)
            val p0 = Path(ROOT_PATH, "k", 4, 'x')
            val p1 = Path(p0, "l", 5, 'y')
            val path = Path(p1, "m", 6, 'z')
            for (sample in listOf(
                CSet(path, "path", "z"),
                CAdd(path, 'x', "Entity")
            )) {
                LOGGER.info(sample)
                val clone = TextReader.read(TextWriter.toText(sample, InvalidRef), InvalidRef, true).first()
                LOGGER.info(clone)
            }
        }

    }

}
