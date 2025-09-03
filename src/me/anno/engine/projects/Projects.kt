package me.anno.engine.projects

import me.anno.config.ConfigRef
import me.anno.config.DefaultConfig
import me.anno.engine.EngineBase
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.saveable.Saveable
import org.apache.logging.log4j.LogManager

@Suppress("MemberVisibilityCanBePrivate")
object Projects {

    private val LOGGER = LogManager.getLogger(Projects::class)

    private val recentProjectCount by ConfigRef("ui.project.numRecent", 10)
    fun getRecentProjects(): ArrayList<ProjectHeader> {
        Saveable.registerCustomClass(GameEngineProject::class)
        val projects = ArrayList<ProjectHeader>()
        val usedFiles = HashSet<FileReference>()
        for (i in 0 until recentProjectCount) {
            val name = DefaultConfig["recent.projects[$i].name"] as? String ?: continue
            val file = DefaultConfig["recent.projects[$i].file", InvalidRef]
            if (file !in usedFiles && file.exists) {
                projects += ProjectHeader(name, file)
                usedFiles += file
            }
        }
        // load projects, which were forgotten because the config was deleted
        if (DefaultConfig["recent.projects.detectAutomatically", true]) {
            try {
                for (folder in EngineBase.workspace.listChildren()) {
                    if (folder !in usedFiles && folder.isDirectory) {
                        val configFile = folder.getChild("Project.json")
                        if (configFile.exists) try {
                            LOGGER.debug("Reading {}", configFile)
                            val config = JsonStringReader.readFirstOrNull(
                                configFile.readTextSync(), folder, GameEngineProject::class, true
                            )
                            if (config != null) {
                                projects += ProjectHeader(config.name.ifBlank { folder.name }, folder)
                                usedFiles += folder
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                LOGGER.warn("Crashed loading projects automatically", e)
            }
        }
        return projects
    }

    fun removeFromRecentProjects(file: FileReference) {
        val recent = getRecentProjects()
        recent.removeAll { it.file == file }
        updateRecentProjects(recent)
    }

    fun addToRecentProjects(project: ProjectHeader) {
        val recent = getRecentProjects()
        recent.add(0, project)
        updateRecentProjects(recent)
    }

    fun updateRecentProjects(recentHeaders: List<ProjectHeader>) {
        val usedFiles = HashSet<FileReference>()
        var i = 0
        for (header in recentHeaders) {
            if (header.file !in usedFiles) {
                DefaultConfig["recent.projects[$i].name"] = header.name
                DefaultConfig["recent.projects[$i].file"] = header.file.absolutePath
                usedFiles += header.file
                if (++i > recentProjectCount) break
            }
        }
        for (j in i until recentProjectCount) {
            DefaultConfig.remove("recent.projects[$j].name")
            DefaultConfig.remove("recent.projects[$j].file")
        }
        DefaultConfig.save("main.config")
    }

    fun addToRecentProjects(name: String, file: FileReference) {
        addToRecentProjects(ProjectHeader(name, file))
    }
}