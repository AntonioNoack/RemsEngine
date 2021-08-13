package me.anno.ecs.prefab

import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.files.LocalFile.toGlobalFile

class CAdd() : Change(2) {

    constructor(path: Path, type: Char, className: String, uuid: String, prefab: FileReference = InvalidRef) : this() {
        this.path = path
        this.type = type
        this.clazzName = className
        this.prefab = prefab
        this.uuid = uuid
    }

    /*constructor(type: String, prefab: FileReference) : this(type) {
        this.prefab = prefab
    }

    constructor(className: String) : this() {
        this.clazzName = className
    }

    constructor(path: IntArray, type: String) : this(Path(path), type)

    constructor(path: Path, className: String) : this() {
        this.clazzName = className
        this.path = path
    }*/

    fun getChildPath(index: Int): Path {
        return path!!.added(uuid!!, index, type)
    }

    var type: Char = 0.toChar()
    var clazzName: String? = null
    var uuid: String? = null
    var prefab: FileReference = InvalidRef

    // name is not used -> could be used as type...

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeChar("type", type)
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
        val loadedInstance = Prefab.loadPrefab(prefab)?.createInstance()
        val newInstance = loadedInstance ?: ISaveable.createOrNull(clazzName ?: return) as PrefabSaveable
        val uuid = uuid
        if (uuid != null) newInstance.name = uuid
        instance.addChildByType(instance.getChildListByType(type).size, type, newInstance)
    }

    override val className: String = "CAdd"
    override val approxSize: Int = 10
    override fun isDefaultValue(): Boolean = false

}
