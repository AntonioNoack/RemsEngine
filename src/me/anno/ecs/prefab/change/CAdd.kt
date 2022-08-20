package me.anno.ecs.prefab.change

import me.anno.Build
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.base.InvalidClassException
import me.anno.io.base.UnknownClassException
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.files.LocalFile.toGlobalFile

class CAdd() : Change() {

    constructor(
        parentPath: Path,
        type: Char,
        clazzName: String,
        nameId: String,
        prefab: FileReference = InvalidRef
    ) : this() {
        this.path = parentPath
        this.type = type
        this.clazzName = clazzName
        this.prefab = prefab
        this.nameId = nameId
    }

    constructor(
        parentPath: Path,
        type: Char,
        clazzName: String,
        prefab: FileReference = InvalidRef
    ) : this(parentPath, type, clazzName, clazzName, prefab)

    fun withPath(path: Path, changeId: Boolean): CAdd {
        val nameId = if (changeId) Path.generateRandomId() else nameId
        return CAdd(path, type, clazzName, nameId, prefab)
    }

    fun getSetterPath(index: Int): Path {
        return path.added(nameId, index, type)
    }

    var type = ' '
    var clazzName = ""
    var nameId = ""
    var prefab: FileReference = InvalidRef

    override fun clone(): CAdd {
        val clone = CAdd()
        clone.path = path
        clone.type = type
        clone.clazzName = clazzName
        clone.prefab = prefab
        clone.nameId = nameId
        return clone
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeChar("type", type)
        writer.writeString("id", nameId)
        writer.writeString("class", clazzName)
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
            "className", "class" -> clazzName = value ?: return
            "prefab" -> prefab = value?.toGlobalFile() ?: InvalidRef
            "name", "id" -> this.nameId = value ?: return
            else -> super.readString(name, value)
        }
    }

    override fun readFile(name: String, value: FileReference) {
        when (name) {
            "prefab" -> prefab = value
            else -> super.readFile(name, value)
        }
    }

    override fun applyChange(instance: PrefabSaveable, depth: Int) {

        // LOGGER.info("adding $clazzName/$nameId with type $type to $path; to ${instance.prefabPath}, ${path == instance.prefabPath}")
        if (prefab != InvalidRef && depth < 0) throw RuntimeException("Circular reference on $prefab")

        val loadedInstance = PrefabCache[prefab, depth]?.createInstance()
        val clazzName = clazzName
        var newInstance = loadedInstance
        if (newInstance == null) {
            when (val maybe = ISaveable.createOrNull(clazzName)) {
                is PrefabSaveable -> newInstance = maybe
                null -> throw UnknownClassException(clazzName)
                else -> throw InvalidClassException("Class \"$clazzName\" does not extend PrefabSaveable")
            }
        }

        val type = type
        val prefab = instance.prefab
        val index = instance.getChildListByType(type).size
        val nameId = nameId
        if (nameId.isNotEmpty() && nameId[0] != '#') {
            // an actual name; names in fbx files and such should not start with # if possible xD
            newInstance.name = nameId
        }

        val path = instance.prefabPath!!.added(nameId, index, type)

        newInstance.changePaths(prefab, path)

        if (Build.isDebug) newInstance.forAll {
            if (it.prefab !== prefab) {
                throw IllegalStateException("Incorrectly changed paths")
            }
        }

        instance.addChildByType(index, type, newInstance)

    }

    override val className = "CAdd"
    override val approxSize: Int = 10
    override fun isDefaultValue(): Boolean = false

}
