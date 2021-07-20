package me.anno.ecs.prefab

import me.anno.ecs.Entity
import me.anno.engine.scene.ScenePrefab
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextWriter
import me.anno.utils.files.LocalFile.toGlobalFile

class EntityPrefab() : Saveable() {

    var changes: List<Change>? = null
    var prefab: FileReference = InvalidRef
    var ownFile: FileReference = InvalidRef

    // for the game runtime, we could save the prefab instance here
    // or maybe even just add the changes, and merge them
    // (we don't need to override twice or more times)

    var history: ChangeHistory? = null

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("prefab", prefab)
        writer.writeObjectList(null, "changes", changes ?: emptyList())
        writer.writeObject(null, "history", history)
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "prefab" -> prefab = value.toGlobalFile()
            else -> super.readString(name, value)
        }
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "changes" -> changes = values.filterIsInstance<Change>()
            else -> super.readObjectArray(name, values)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "history" -> history = value as? ChangeHistory ?: return
            else -> super.readObject(name, value)
        }
    }

    fun createInstance(): Entity {
        val entity = PrefabInspector.loadChanges(prefab)?.run { createInstance() } ?: Entity()
        val changes = changes ?: emptyList()
        val changes2 = changes.groupBy { it.className }.map { "${it.value.size}x ${it.key}" }
        println("creating entity instance from ${changes.size} changes, $changes2")
        for (change in changes) {
            change.apply(entity)
        }
        println("created instance '${entity.name}' has ${entity.children.size} children and ${entity.components.size} components")
        return entity
    }

    override val className: String = "EntityPrefab"
    override val approxSize: Int = 100_000_000
    override fun isDefaultValue(): Boolean =
        (changes == null || changes!!.isEmpty()) && prefab == InvalidRef && history == null

    companion object {

        fun createOrLoadScene(file: FileReference): EntityPrefab {
            val prefab = PrefabInspector.loadChanges(file) ?: EntityPrefab()
            prefab.prefab = ScenePrefab
            prefab.ownFile = file
            if (!file.exists) file.writeText(TextWriter.toText(prefab, false))
            return prefab
        }

    }

}