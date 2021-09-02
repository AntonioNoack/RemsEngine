package me.anno.ecs.prefab

import me.anno.ecs.prefab.PrefabCache.loadPrefab
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.files.LocalFile.toGlobalFile

class CAdd() : Change(2) {

    constructor(
        path: Path,
        type: Char,
        className: String,
        name: String = className,
        prefab: FileReference = InvalidRef
    ) : this() {
        this.path = path
        this.type = type
        this.clazzName = className
        this.prefab = prefab
        this.name = name
    }

    fun getChildPath(index: Int): Path {
        return path.added(name!!, index, type)
    }

    var type: Char = ' '
    var clazzName: String? = null
    var name: String? = null
    var prefab: FileReference = InvalidRef

    override fun clone(): Change {
        val clone = CAdd()
        clone.path = path
        clone.type = type
        clone.clazzName = clazzName
        clone.prefab = prefab
        clone.name = name
        return clone
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeChar("type", type)
        writer.writeString("name", name)
        writer.writeString("className", clazzName)
        writer.writeString("prefab", prefab.toString())
    }

    override fun readChar(name: String, value: Char) {
        when (name) {
            "type" -> type = value
            else -> super.readChar(name, value)
        }
    }

    override fun readString(name: String, value: String?) {
        when (name) {
            "className" -> clazzName = value
            "prefab" -> prefab = value?.toGlobalFile() ?: InvalidRef
            "name" -> this.name = value ?: return
            else -> super.readString(name, value)
        }
    }

    override fun readFile(name: String, value: FileReference) {
        when (name) {
            "prefab" -> prefab = value
            else -> super.readFile(name, value)
        }
    }

    override fun applyChange(instance: PrefabSaveable) {
        // LOGGER.info("adding $clazzName with type $type")
        val loadedInstance = loadPrefab(prefab)?.createInstance()
        val newInstance = loadedInstance
            ?: ISaveable.createOrNull(clazzName ?: return) as? PrefabSaveable
            ?: throw RuntimeException("Class $clazzName was not found")
        val name = name; if (name != null) newInstance.name = name
        instance.addChildByType(instance.getChildListByType(type).size, type, newInstance)
    }

    override val className: String = "CAdd"
    override val approxSize: Int = 10
    override fun isDefaultValue(): Boolean = false

}
