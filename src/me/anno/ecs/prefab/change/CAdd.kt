package me.anno.ecs.prefab.change

import me.anno.ecs.prefab.PrefabCache.loadPrefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.files.LocalFile.toGlobalFile

class CAdd() : Change(2) {

    constructor(
        parentPath: Path,
        type: Char,
        clazzName: String,
        name: String? = clazzName,
        prefab: FileReference = InvalidRef
    ) : this() {
        this.path = parentPath
        this.type = type
        this.clazzName = clazzName
        this.prefab = prefab
        this.name = name
    }

    constructor(
        parentPath: Path,
        type: Char,
        clazzName: String,
        prefab: FileReference = InvalidRef
    ) : this(parentPath, type, clazzName, clazzName, prefab)

    override fun withPath(path: Path): Change {
        return CAdd(path, type, clazzName!!, name, prefab)
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

    override fun applyChange(instance: PrefabSaveable, chain: MutableSet<FileReference>?) {

        // LOGGER.info("adding $clazzName with type $type")
        if (prefab != InvalidRef && chain?.add(prefab) == false) throw RuntimeException("Circular Reference on $chain: $prefab")

        val loadedInstance = loadPrefab(prefab, chain)?.createInstance()
        var newInstance = loadedInstance
        if (newInstance == null) {
            val maybe = ISaveable.createOrNull(clazzName ?: return)
            if (maybe is PrefabSaveable) {
                newInstance = maybe
            } else {
                throw RuntimeException("Class $clazzName is not PrefabSaveable")
            }
        }


        val prefab = instance.prefab
        val index = instance.getChildListByType(type).size
        val name = name; if (name != null) newInstance.name = name
        val path = instance.prefabPath!!.added(name ?: className, index, type)

        newInstance.changePaths(prefab, path)

        newInstance.forAll {
            if (it.prefab !== prefab) {
                throw IllegalStateException("Incorrectly changed paths")
            }
        }

        instance.addChildByType(index, type, newInstance)

    }

    override val className: String = "CAdd"
    override val approxSize: Int = 10
    override fun isDefaultValue(): Boolean = false

}
