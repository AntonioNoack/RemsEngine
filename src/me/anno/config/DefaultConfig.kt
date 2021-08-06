package me.anno.config

import me.anno.config.DefaultStyle.baseTheme
import me.anno.io.config.ConfigBasics
import me.anno.io.text.TextReader
import me.anno.io.utils.StringMap
import me.anno.objects.*
import me.anno.objects.attractors.EffectColoring
import me.anno.objects.attractors.EffectMorphing
import me.anno.objects.effects.MaskLayer
import me.anno.objects.geometric.Circle
import me.anno.objects.geometric.Polygon
import me.anno.objects.meshes.Mesh
import me.anno.objects.modes.UVProjection
import me.anno.objects.particles.ParticleSystem
import me.anno.objects.particles.TextParticles
import me.anno.objects.text.Text
import me.anno.objects.text.Timer
import me.anno.studio.StudioBase.Companion.workspace
import me.anno.studio.project.Project
import me.anno.ui.base.Font
import me.anno.ui.style.Style
import me.anno.utils.Clock
import me.anno.utils.OS
import me.anno.utils.Warning
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import java.io.File

object DefaultConfig : StringMap() {

    private val LOGGER = LogManager.getLogger(DefaultConfig::class)

    lateinit var style: Style

    var createDefaults: (config: DefaultConfig) -> Unit = {}

    var hasInit = false

    fun init() {

        hasInit = true

        val tick = Clock()

        this["style"] = "dark"

        createDefaults(this)

        var newConfig: StringMap = this
        try {
            newConfig = ConfigBasics.loadConfig("main.config", this, true)
            putAll(newConfig)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val stylePath = newConfig["style"]?.toString() ?: "dark"
        style = baseTheme.getStyle(stylePath)

        // not completely true; is loading some classes, too
        tick.stop("reading the config")

    }

    fun defineDefaultFileAssociations(){

        addImportMappings(
            "Image",
            "png", "jpg", "jpeg", "tiff", "webp", "svg", "ico", "psd", "bmp", "jp2", "tga"
        )
        addImportMappings("PDF", "pdf")
        addImportMappings("Cubemap-Equ", "hdr")
        addImportMappings(
            "Video",
            "mp4", "m4p", "m4v", "gif", "webm",
            "mpeg", "mp2", "mpg", "mpe", "mpv", "svi", "3gp", "3g2", "roq",
            "nsv", "f4v", "f4p", "f4a", "f4b",
            "avi", "flv", "vob", "wmv", "mkv", "ogg", "ogv", "drc",
            "mov", "qt", "mts", "m2ts", "ts", "rm", "rmvb", "viv", "asf", "amv"
        )
        addImportMappings("Text", "txt")
        addImportMappings("Mesh", "obj", "fbx", "dae", "gltf", "glb", "md2", "md5mesh", "vox")
        // not yet supported
        // addImportMappings("Markdown", "md")
        addImportMappings("Audio", "mp3", "wav", "m4a", "ogg")
        addImportMappings("URL", "url", "lnk", "desktop")
        addImportMappings("Container", "unitypackage", "zip", "7z", "tar", "gz", "xz", "rar", "bz2", "xar", "oar")

    }

    override fun get(key: String): Any? {
        if (!hasInit) Warning.warn("Too early access of DefaultConfig[$key]")
        return super.get(key)
    }

    /*fun save() {
        this.wasChanged = false
        baseTheme.values.wasChanged = false
        ConfigBasics.save("main.config", this.toString())
        ConfigBasics.save("style.config", baseTheme.values.toString())
    }*/

    fun newInstances() {

        val tick = Clock()

        val newInstances: Map<String, Transform> = mapOf(
            "Mesh" to Mesh(getReference(OS.documents, "monkey.obj"), null),
            "Array" to GFXArray(),
            "Image / Audio / Video" to Video(),
            "Polygon" to Polygon(null),
            "Rectangle" to Rectangle.create(),
            "Circle" to Circle(null),
            "Folder" to Transform(),
            // "Linked Object" to SoftLink(), // non-default, can be created using drag n drop
            "Mask" to MaskLayer.create(null, null),
            "Text" to Text("Text"),
            "Timer" to Timer(),
            "Cubemap" to run {
                val cube = Video()
                cube.uvProjection *= UVProjection.TiledCubemap
                cube.scale.set(Vector3f(1000f, 1000f, 1000f))
                cube
            },
            "Cube" to run {
                val cube = Polygon()
                cube.name = "Cube"
                cube.autoAlign = true
                cube.is3D = true
                cube.vertexCount.set(4)
                cube
            },
            "Camera" to Camera(),
            "Particle System" to run {
                val ps = ParticleSystem()
                ps.name = "Particles"
                Circle(ps)
                ps.timeOffset.value = -5.0
                ps
            },
            "Text Particles" to TextParticles(),
            "Effect: Coloring" to EffectColoring(),
            "Effect: Morphing" to EffectMorphing()
        )

        this["createNewInstancesList"] =
            StringMap(16, false)
                .addAll(newInstances)

        tick.stop("new instances list")

    }

    data class ProjectHeader(val name: String, val file: FileReference){
        constructor(name: String, file: File): this(name, getReference(file))
    }

    private val recentProjectCount = 10
    fun getRecentProjects(): ArrayList<ProjectHeader> {
        val projects = ArrayList<ProjectHeader>()
        val usedFiles = HashSet<FileReference>()
        for (i in 0 until recentProjectCount) {
            val name = this["recent.projects[$i].name"] as? String ?: continue
            val file = getReference(this["recent.projects[$i].file"] as? String ?: continue)
            if (file !in usedFiles) {
                projects += ProjectHeader(name, file)
                usedFiles += file
            }
        }
        // load projects, which were forgotten because the config was deleted
        if (DefaultConfig["recent.projects.detectAutomatically", true]) {
            try {
                for (folder in workspace.listChildren() ?: emptyList()) {
                    if (folder !in usedFiles) {
                        if (folder.isDirectory) {
                            val configFile = getReference(folder, "config.json")
                            if (configFile.exists) {
                                try {
                                    val config = TextReader.read(configFile).firstOrNull() as? StringMap
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

    fun addToRecentProjects(project: Project) {
        addToRecentProjects(ProjectHeader(project.name, project.file))
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

    fun updateRecentProjects(recent: List<ProjectHeader>) {
        val usedFiles = HashSet<FileReference>()
        var i = 0
        for (projectI in recent) {
            if (projectI.file !in usedFiles) {
                this["recent.projects[$i].name"] = projectI.name
                this["recent.projects[$i].file"] = projectI.file.absolutePath
                usedFiles += projectI.file
                if (++i > recentProjectCount) break
            }
        }
        for (j in i until recentProjectCount) {
            remove("recent.projects[$i].name")
            remove("recent.projects[$i].file")
        }
        save("main.config")
    }

    fun addImportMappings(result: String, vararg extensions: String) {
        for (extension in extensions) {
            this["import.mapping.$extension"] = result
        }
    }

    val defaultFontName get() = this["defaultFont"] as? String ?: "Verdana"
    val defaultFont get() = Font(defaultFontName, 15f, false, false)

}