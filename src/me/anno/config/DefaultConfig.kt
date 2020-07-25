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
import me.anno.objects.particles.ParticleSystem
import me.anno.ui.style.Style
import me.anno.utils.OS
import me.anno.utils.f3
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import java.io.File

object DefaultConfig: StringMap() {

    private val LOGGER = LogManager.getLogger(DefaultConfig::class)

    init {
        init()
    }

    lateinit var style: Style

    fun init() {

        val t0 = System.nanoTime()

        this["style"] = "dark"
        this["ffmpeg.path"] = File(OS.home, "Downloads\\lib\\ffmpeg\\bin\\ffmpeg.exe")
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
        addImportMappings("Cubemap", "hdr")
        addImportMappings("Video", "mp4", "gif", "mpeg", "avi", "flv", "wmv")
        addImportMappings("Text", "txt")
        // not yet supported
        // addImportMappings("Markdown", "md")
        addImportMappings("Audio", "mp3", "wav", "ogg", "m4a")

        this["import.mapping.*"] = "Text"

        val newInstances: Map<String, Transform> = mapOf(
            "Mesh" to Mesh(File(OS.documents, "monkey.obj"), null),
            "Video" to Video(File(""), null),
            "Image" to Image(File(""), null),
            "Polygon" to Polygon(null),
            "Circle" to Circle(null),
            "Folder" to Transform(),
            "Mask" to {
                val mask = MaskLayer(null)
                Circle(mask).innerRadius.set(0.5f)
                Polygon(mask)
                mask
            }(),
            "Text" to Text("Text", null),
            "Cubemap" to {
                val cube = Cubemap(File(""), null)
                cube.scale.set(Vector3f(1000f, 1000f, 1000f))
                cube
            }(),
            "Cube" to {
                val cube = Polygon(null)
                cube.name = "Cube"
                cube.autoAlign = true
                cube.scale.set(Vector3f(1f, 1f, 1f))
                cube.vertexCount.set(4)
                cube
            }(),
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

        val newConfig = ConfigBasics.loadConfig("main.config", this, true)
        if(newConfig !== this){
            putAll(newConfig)
        }

        val stylePath = newConfig["style"]?.toString() ?: "dark"
        style = baseTheme.getStyle(stylePath)

        ActionManager.init()

        val t1 = System.nanoTime()
        // not completely true; is loading some classes, too
        LOGGER.info("Used ${((t1-t0)*1e-9f).f3()}s to read the config")

    }

    fun addImportMappings(result: String, vararg extensions: String){
        for(extension in extensions){
            this["import.mapping.$extension"] = result
        }
    }

    val defaultFont get() = this["defaultFont"] as? String ?: "Verdana"

}