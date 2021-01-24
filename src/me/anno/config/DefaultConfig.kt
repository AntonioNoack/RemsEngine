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
import me.anno.studio.rems.RemsStudio.workspace
import me.anno.studio.project.Project
import me.anno.ui.base.Font
import me.anno.ui.style.Style
import me.anno.utils.OS
import me.anno.utils.FloatFormat.f3
import me.anno.utils.Warning
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

        val t0 = System.nanoTime()

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

        val t1 = System.nanoTime()
        // not completely true; is loading some classes, too
        LOGGER.info("Used ${((t1 - t0) * 1e-9f).f3()}s to read the config")

    }

    override fun get(key: String): Any? {
        if(!hasInit) Warning.warn("Too early access of DefaultConfig[$key]")
        return super.get(key)
    }

    /*fun save() {
        this.wasChanged = false
        baseTheme.values.wasChanged = false
        ConfigBasics.save("main.config", this.toString())
        ConfigBasics.save("style.config", baseTheme.values.toString())
    }*/

    fun newInstances() {

        val t0 = System.nanoTime()

        val newInstances: Map<String, Transform> = mapOf(
            "Mesh" to Mesh(File(OS.documents, "monkey.obj"), null),
            "Array" to GFXArray(),
            "Image / Audio / Video" to Video(File(""), null),
            "Polygon" to Polygon(null),
            "Rectangle" to Rectangle.create(),
            "Circle" to Circle(null),
            "Folder" to Transform(),
            "Mask" to MaskLayer.create(null, null),
            "Text" to Text("Text", null),
            "Timer" to Timer(null),
            "Cubemap" to {
                val cube = Video(File(""), null)
                cube.uvProjection = UVProjection.TiledCubemap
                cube.scale.set(Vector3f(1000f, 1000f, 1000f))
                cube
            }(),
            "Cube" to {
                val cube = Polygon(null)
                cube.name = "Cube"
                cube.autoAlign = true
                cube.is3D = true
                cube.vertexCount.set(4)
                cube
            }(),
            "Camera" to Camera(),
            "Particle System" to {
                val ps = ParticleSystem(null)
                ps.name = "Particles"
                Circle(ps)
                ps.timeOffset = -5.0
                ps
            }(),
            "Text Particles" to TextParticles(),
            "Effect: Coloring" to EffectColoring(),
            "Effect: Morphing" to EffectMorphing()
        )

        this["createNewInstancesList"] =
            StringMap(16, false, saveDefaultValues = true)
                .addAll(newInstances)

        val t1 = System.nanoTime()
        LOGGER.info("Used ${((t1 - t0) * 1e-9).f3()}s for new instances list")

    }

    class ProjectHeader(val name: String, val file: File)

    private val recentProjectCount = 10
    fun getRecentProjects(): ArrayList<ProjectHeader> {
        val projects = ArrayList<ProjectHeader>()
        val usedFiles = HashSet<File>()
        for (i in 0 until recentProjectCount) {
            val name = this["recent.projects[$i].name"] as? String ?: continue
            val file = File(this["recent.projects[$i].file"] as? String ?: continue)
            if (file !in usedFiles) {
                projects += ProjectHeader(name, file)
                usedFiles += file
            }
        }
        // load projects, which were forgotten because the config was deleted
        if (DefaultConfig["recent.projects.detectAutomatically", true]) {
            try {
                for (folder in workspace.listFiles() ?: emptyArray()) {
                    if (folder !in usedFiles) {
                        if (folder.isDirectory) {
                            val configFile = File(folder, "config.json")
                            if (configFile.exists()) {
                                try {
                                    val config = TextReader.fromText(configFile.readText()).firstOrNull() as? StringMap
                                    if (config != null) {
                                        projects += ProjectHeader(config["general.name", folder.name], folder)
                                        usedFiles += folder
                                    }
                                } catch (e: Exception){
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

    fun removeFromRecentProjects(file: File) {
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
        val usedFiles = HashSet<File>()
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