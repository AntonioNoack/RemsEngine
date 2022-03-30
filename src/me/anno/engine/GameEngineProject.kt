package me.anno.engine

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.studio.StudioBase
import me.anno.utils.files.LocalFile.toGlobalFile

class GameEngineProject() : NamedSaveable() {

    // todo save the config, if something changes

    companion object {
        fun readOrCreate(location: FileReference?): GameEngineProject? {
            location ?: return null
            if (location == InvalidRef) return null
            return if (location.exists) {
                if (location.isDirectory) {
                    val configFile = location.getChild("config.json")
                    if (configFile.exists) {
                        val instance = TextReader.readFirstOrNull<GameEngineProject>(configFile)
                        instance?.location = location
                        instance
                    } else {
                        val project = GameEngineProject(location)
                        configFile.writeText(TextWriter.toText(project))
                        project
                    }
                } else {
                    // probably the config file
                    readOrCreate(location.getParent())
                }
            } else GameEngineProject(location)
        }
    }

    constructor(location: FileReference) : this() {
        this.location = location
        location.tryMkdirs()
    }

    var location: FileReference = InvalidRef // a folder
    var lastScene: FileReference = InvalidRef

    fun forAllPrefabs(run: (FileReference, Prefab) -> Unit) {
        forAllFiles { file ->
            val prefab = PrefabCache.getPrefab(file)
            if (prefab != null) run(file, prefab)
        }
    }

    fun forAllFiles(run: (FileReference) -> Unit) {
        forAllFiles(location, 10, run)
    }

    fun forAllFiles(folder: FileReference, maxDepth: Int, run: (FileReference) -> Unit) {
        try {
            // to do go into things as well???
            for (child in folder.listChildren() ?: return) {
                if (child.isDirectory) {
                    if (maxDepth > 0) {
                        forAllFiles(child, maxDepth - 1, run)
                    }
                } else {
                    run(child)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun init() {
        StudioBase.workspace = location
        // if last scene is invalid, create a valid scene
        if (lastScene == InvalidRef) {
            lastScene = location.getChild("Scene.json")
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("lastScene", lastScene)
        // location doesn't really need to be saved
    }

    override fun readString(name: String, value: String?) {
        when (name) {
            "lastScene" -> lastScene = value?.toGlobalFile() ?: InvalidRef
            else -> super.readString(name, value)
        }
    }

    override fun readFile(name: String, value: FileReference) {
        when (name) {
            "lastScene" -> lastScene = value
            else -> super.readFile(name, value)
        }
    }

    override val className: String = "GameEngineProject"
    override val approxSize: Int = 1_000_000

}