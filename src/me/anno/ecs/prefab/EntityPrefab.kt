package me.anno.ecs.prefab

import me.anno.ecs.Entity
import me.anno.engine.scene.ScenePrefab
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.mesh.vox.VOXReader
import me.anno.utils.files.LocalFile.toGlobalFile
import org.apache.logging.log4j.LogManager

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
        // LOGGER.info("Requesting instance $ownFile: $prefab")
        return createInstance(prefab, changes, HashSet())
    }

    fun createInstance(chain: MutableSet<FileReference>?): Entity {
        // LOGGER.info("Requesting instance $ownFile: $prefab")
        return createInstance(prefab, changes, chain)
    }

    override val className: String = "EntityPrefab"
    override val approxSize: Int = 100_000_000
    override fun isDefaultValue(): Boolean =
        (changes == null || changes!!.isEmpty()) && prefab == InvalidRef && history == null

    companion object {

        private val LOGGER = LogManager.getLogger(EntityPrefab::class)

        private fun createInstance(
            superPrefab: FileReference,
            changes0: List<Change>?,
            chain: MutableSet<FileReference>?
        ): Entity {
            // LOGGER.info("creating instance from $superPrefab")
            val entity = createSuperInstance(superPrefab, chain)
            // val changes2 = (changes0 ?: emptyList()).groupBy { it.className }.map { "${it.value.size}x ${it.key}" }
            // LOGGER.info("  creating entity instance from ${changes0?.size ?: 0} changes, $changes2")
            if (changes0 != null) {
                for ((index, change) in changes0.withIndex()) {
                    try {
                        change.apply(entity)
                    } catch (e: Exception) {
                        LOGGER.warn("Change $index, $change failed")
                        throw e
                    }
                }
            }
            // LOGGER.info("  created instance '${entity.name}' has ${entity.children.size} children and ${entity.components.size} components")
            return entity
        }

        private fun loadVOXModel(resource: FileReference): EntityPrefab? {
            return try {
                VOXReader().read(resource).toEntityPrefab()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        private fun loadJson(resource: FileReference): EntityPrefab? {
            return try {
                val read = TextReader.read(resource)
                val prefab = read.firstOrNull() as? EntityPrefab
                if(prefab == null) LOGGER.warn("No EntityPrefab found in $resource! $read")
                else LOGGER.info("Read ${prefab.changes?.size} changes from $resource")
                prefab
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun loadPrefab(resource: FileReference): EntityPrefab? {
            return if (resource != InvalidRef && resource.exists && !resource.isDirectory) {
                val signature = Signature.find(resource)
                // LOGGER.info("resource $resource has signature $signature")
                when (signature?.name) {
                    "vox" -> loadVOXModel(resource)
                    // todo obj, other 3d formats
                    else -> {
                        when (resource.extension.lowercase()) {
                            "vox" -> loadVOXModel(resource)
                            else -> loadJson(resource)
                        }
                    }
                }
            } else null
        }

        private fun createSuperInstance(prefab: FileReference, chain: MutableSet<FileReference>?): Entity {
            if (chain != null) {
                if (prefab in chain) throw RuntimeException()
                chain.add(prefab)
            }
            // LOGGER.info("chain: $chain")
            return loadPrefab(prefab)?.createInstance(chain) ?: return Entity()
        }

        fun loadScenePrefab(file: FileReference): EntityPrefab {
            // LOGGER.info("loading scene")
            val prefab = loadPrefab(file) ?: EntityPrefab()
            prefab.prefab = ScenePrefab
            prefab.ownFile = file
            if (!file.exists) file.writeText(TextWriter.toText(prefab, false))
            return prefab
        }

    }

}