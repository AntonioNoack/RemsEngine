package me.anno.config

import me.anno.config.DefaultStyle.baseTheme
import me.anno.input.ActionManager
import me.anno.io.config.ConfigBasics
import me.anno.io.utils.StringMap
import me.anno.objects.*
import me.anno.objects.effects.MaskLayer
import me.anno.objects.geometric.Circle
import me.anno.objects.geometric.Polygon
import me.anno.objects.particles.ParticleSystem
import me.anno.ui.style.Style
import me.anno.utils.f3
import org.joml.Vector3f
import java.io.File

object DefaultConfig: StringMap() {

    init {
        init()
    }

    lateinit var style: Style

    fun init() {

        val t0 = System.nanoTime()

        this["style"] = "dark"
        this["ffmpegPath"] = "C:\\Users\\Antonio\\Downloads\\lib\\ffmpeg\\bin\\ffmpeg.exe"
        this["tooltip.reactionTime"] = 300
        this["lastUsed.fonts.count"] = 5
        this["default.video.nearest"] = false
        this["default.image.nearest"] = false

        this["grid.axis.x.color"] = "#ff7777"
        this["grid.axis.y.color"] = "#77ff77"
        this["grid.axis.z.color"] = "#7777ff"
        this["format.svg.stepsPerDegree"] = 0.1f

        this["display.colorDepth"] = 8

        addImportMappings("Transform", "json")
        addImportMappings("Image", "png", "jpg", "jpeg", "tiff", "webp", "svg")
        addImportMappings("Cubemap", "hdr")
        addImportMappings("Video", "mp4", "gif", "mpeg", "avi")
        addImportMappings("Text", "txt")
        addImportMappings("Markdown", "md")
        addImportMappings("Audio", "mp3", "wav", "ogg")

        this["import.mapping.*"] = "Text"

        this["createNewInstancesList"] = StringMap(16, false, saveDefaultValues = true)
            .addAll(mapOf(
                "Video" to Video(File(""), null),
                "Image" to Image(File(""), null),
                "Polygon" to Polygon(null),
                "Circle" to Circle(null),
                "Folder" to Transform(),
                "Mask" to MaskLayer(null),
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
                    cube.vertexCount.set(4f)
                    cube
                }(),
                "Particle System" to {
                    val ps = ParticleSystem(null)
                    ps.name = "PSystem"
                    Circle(ps)
                    ps.timeOffset = -5f
                    ps
                }()
            ))

        val newConfig = ConfigBasics.loadConfig("main.config", this, true)
        if(newConfig !== this){
            putAll(newConfig)
        }

        val stylePath = newConfig["style"]?.toString() ?: "dark"
        style = baseTheme.getStyle(stylePath)

        ActionManager.init()

        val t1 = System.nanoTime()
        // not completely true; is loading some classes, too
        println("[INFO] Used ${((t1-t0)*1e-9f).f3()}s to read the config")

    }

    fun addImportMappings(result: String, vararg extensions: String){
        for(extension in extensions){
            this["import.mapping.$extension"] = result
        }
    }

    val defaultFont get() = this["defaultFont"] as? String ?: "Verdana"

}