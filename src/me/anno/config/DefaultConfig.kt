package me.anno.config

import me.anno.config.DefaultStyle.baseTheme
import me.anno.input.ActionManager
import me.anno.io.config.ConfigBasics
import me.anno.io.utils.StringMap
import me.anno.objects.*
import me.anno.objects.effects.MaskLayer
import me.anno.objects.geometric.Circle
import me.anno.objects.geometric.Polygon
import me.anno.objects.meshes.Mesh
import me.anno.objects.modes.UVProjection
import me.anno.objects.particles.ParticleSystem
import me.anno.studio.project.Project
import me.anno.ui.style.Style
import me.anno.utils.OS
import me.anno.utils.f3
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import java.io.File
import java.lang.Exception
import kotlin.math.max
import kotlin.math.min

object DefaultConfig: StringMap() {

    private val LOGGER = LogManager.getLogger(DefaultConfig::class)

    init {
        init()
    }

    lateinit var style: Style

    fun init() {

        val t0 = System.nanoTime()

        this["style"] = "dark"
        this["ffmpeg.path"] = File(OS.downloads, "lib\\ffmpeg\\bin\\ffmpeg.exe") // I'm not sure about that one ;)
        this["tooltip.reactionTime"] = 300
        this["lastUsed.fonts.count"] = 5
        this["default.video.nearest"] = false
        this["default.image.nearest"] = false

        this["grid.axis.x.color"] = "#ff7777"
        this["grid.axis.y.color"] = "#77ff77"
        this["grid.axis.z.color"] = "#7777ff"
        this["format.svg.stepsPerDegree"] = 0.1f

        this["target.resolutions.default"] = "1920x1080"
        this["target.resolutions.defaultValues"] = "1920x1080,1920x1200,720x480,2560x1440,3840x2160"
        this["target.resolutions.sort"] = 1 // 1 = ascending order, -1 = descending order, 0 = don't sort

        this["display.colorDepth"] = 8

        this["rendering.useMSAA"] = true // should not be deactivated, unless... idk...
        this["editor.useMSAA"] = true // can be deactivated for really weak GPUs

        addImportMappings("Transform", "json")
        addImportMappings("Image", "png", "jpg", "jpeg", "tiff", "webp", "svg", "ico")
        addImportMappings("Cubemap-Equ", "hdr")
        addImportMappings("Video", "mp4", "gif", "mpeg", "avi", "flv", "wmv")
        addImportMappings("Text", "txt")
        addImportMappings("Mesh", "obj", "fbx", "dae")
        // not yet supported
        // addImportMappings("Markdown", "md")
        addImportMappings("Audio", "mp3", "wav", "ogg", "m4a")

        this["import.mapping.*"] = "Text"

        newInstances()

        var newConfig: StringMap = this
        try {
            newConfig = ConfigBasics.loadConfig("main.config", this, true)
            putAll(newConfig)
        } catch (e: Exception){
            e.printStackTrace()
        }

        val stylePath = newConfig["style"]?.toString() ?: "dark"
        style = baseTheme.getStyle(stylePath)

        ActionManager.init()

        val t1 = System.nanoTime()
        // not completely true; is loading some classes, too
        LOGGER.info("Used ${((t1-t0)*1e-9f).f3()}s to read the config")

    }

    fun save(){
        ConfigBasics.save("main.config", this.toString())
    }

    fun newInstances(){

        // removing this block makes the studio start 0.1s slower... wtf...

        val t0 = System.nanoTime()

        val newInstances: Map<String, Transform> = mapOf(
            "Mesh" to Mesh(File(OS.documents, "monkey.obj"), null),
            "Array" to GFXArray(),
            "Video" to Video(File(""), null),
            // "Image" to Video(File(""), null),
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
                ps.name = "PSystem"
                Circle(ps)
                ps.timeOffset = -5.0
                ps
            }()
        )

        this["createNewInstancesList"] =
            StringMap(16, false, saveDefaultValues = true)
                .addAll(newInstances)

        val t1 = System.nanoTime()
        LOGGER.info("Used ${((t1-t0)*1e-9).f3()}s for new instances list")

    }

    class ProjectHeader(val name: String, val file: File)

    private val recentProjectCount = 10
    fun getRecentProjects(): ArrayList<ProjectHeader> {
        val projects = ArrayList<ProjectHeader>()
        val usedFiles = HashSet<File>()
        for(i in 0 until recentProjectCount){
            val name = this["recent.projects[$i].name"] as? String ?: continue
            val file = File(this["recent.projects[$i].file"] as? String ?: continue)
            if(file !in usedFiles){
                projects += ProjectHeader(name, file)
                usedFiles += file
            }
        }
        return projects
    }

    fun addToRecentProjects(project: Project){
        addToRecentProjects(ProjectHeader(project.name, project.file))
    }

    fun removeFromRecentProjects(file: File){
        val recent = getRecentProjects()
        recent.removeIf { it.file == file }
        updateRecentProjects(recent)
    }

    fun addToRecentProjects(project: ProjectHeader){
        val recent = getRecentProjects()
        recent.add(0, project)
        updateRecentProjects(recent)
    }

    fun updateRecentProjects(recent: List<ProjectHeader>){
        val usedFiles = HashSet<File>()
        var i = 0
        for(projectI in recent){
            if(projectI.file !in usedFiles){
                this["recent.projects[$i].name"] = projectI.name
                this["recent.projects[$i].file"] = projectI.file.absolutePath
                usedFiles += projectI.file
                if(++i > recentProjectCount) break
            }
        }
        for(j in i until recentProjectCount){
            remove("recent.projects[$i].name")
            remove("recent.projects[$i].file")
        }
        save()
    }

    fun addImportMappings(result: String, vararg extensions: String){
        for(extension in extensions){
            this["import.mapping.$extension"] = result
        }
    }

    val defaultFont get() = this["defaultFont"] as? String ?: "Verdana"

}