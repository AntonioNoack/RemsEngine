package me.anno.studio

import me.anno.config.DefaultConfig
import me.anno.io.files.FileReference
import me.anno.io.text.TextReader
import me.anno.io.utils.StringMap
import org.apache.logging.log4j.LogManager

object Projects {

    private val LOGGER = LogManager.getLogger(Projects::class)

    private val recentProjectCount = 10
    fun getRecentProjects(): ArrayList<ProjectHeader> {
        val projects = ArrayList<ProjectHeader>()
        val usedFiles = HashSet<FileReference>()
        for (i in 0 until recentProjectCount) {
            val name = DefaultConfig["recent.projects[$i].name"] as? String ?: continue
            val file = FileReference.getReference(DefaultConfig["recent.projects[$i].file"] as? String ?: continue)
            if (file !in usedFiles) {
                projects += ProjectHeader(name, file)
                usedFiles += file
            }
        }
        // load projects, which were forgotten because the config was deleted
        if (DefaultConfig["recent.projects.detectAutomatically", true]) {
            try {
                for (folder in StudioBase.workspace.listChildren() ?: emptyList()) {
                    if (folder !in usedFiles) {
                        if (folder.isDirectory) {
                            val configFile = FileReference.getReference(folder, "config.json")
                            if (configFile.exists) {
                                try {
                                    val config = TextReader.read(configFile, folder, true).firstOrNull() as? StringMap
                                    if (config != null) {
                                        projects += ProjectHeader(config["general.name", folder.name], folder)
                                        usedFiles += folder
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
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
        recent.removeIf { it.file == file }
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
            DefaultConfig.remove("recent.projects[$i].name")
            DefaultConfig.remove("recent.projects[$i].file")
        }
        DefaultConfig.save("main.config")
    }

    fun addToRecentProjects(name: String, file: FileReference) {
        addToRecentProjects(ProjectHeader(name, file))
    }

}