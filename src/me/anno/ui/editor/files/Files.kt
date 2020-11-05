package me.anno.ui.editor.files

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.objects.Text
import me.anno.objects.Transform
import me.anno.objects.Transform.Companion.toTransform
import me.anno.objects.Video
import me.anno.objects.meshes.Mesh
import me.anno.objects.modes.UVProjection
import me.anno.studio.RemsStudio
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.utils.LOGGER
import me.anno.utils.StringHelper.getImportType
import org.joml.Vector3f
import java.io.File
import kotlin.concurrent.thread

fun addChildFromFile(parent: Transform?, file: File, callback: (Transform) -> Unit, depth: Int = 0) {
    if (file.isDirectory) {
        val directory = Transform(parent)
        directory.name = file.name
        if (depth < DefaultConfig["import.depth.max", 3]) {
            thread {
                file.listFiles()?.filter { !it.name.startsWith(".") }?.forEach {
                    addEvent {
                        addChildFromFile(directory, it, callback, depth + 1)
                    }
                }
            }
        }
    } else {
        val name = file.name
        when (file.extension.getImportType()) {
            "Transform" -> thread {
                val text = file.readText()
                try {
                    val transform = text.toTransform()
                    if (transform == null) {
                        LOGGER.warn("JSON didn't contain Transform!")
                        addText(name, parent, text, callback)
                    } else {
                        addEvent {
                            parent?.addChild(transform)
                            GFX.select(transform)
                            callback(transform)
                            RemsStudio.onLargeChange()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    LOGGER.warn("Didn't understand JSON! ${e.message}")
                    addText(name, parent, text, callback)
                }
            }
            "Cubemap-Equ" -> {
                val cube = Video(file, parent)
                cube.scale.set(Vector3f(1000f, 1000f, 1000f))
                cube.uvProjection = UVProjection.Equirectangular
                cube.name = name
                GFX.select(cube)
                callback(cube)
                RemsStudio.onLargeChange()
            }
            "Cubemap-Tiles" -> {
                val cube = Video(file, parent)
                cube.scale.set(Vector3f(1000f, 1000f, 1000f))
                cube.uvProjection = UVProjection.TiledCubemap
                cube.name = name
                GFX.select(cube)
                callback(cube)
                RemsStudio.onLargeChange()
            }
            "Video", "Image", "Audio" -> {// the same, really ;)
                // rather use a list of keywords?
                val video = Video(file, parent)
                val fName = file.name
                video.name = fName
                if (DefaultConfig["import.decideCubemap", true]) {
                    if (fName.contains("360", true)) {
                        video.scale.set(Vector3f(1000f, 1000f, 1000f))
                        video.uvProjection = UVProjection.Equirectangular
                    } else if (fName.contains("cubemap", true)) {
                        video.scale.set(Vector3f(1000f, 1000f, 1000f))
                        video.uvProjection = UVProjection.TiledCubemap
                    }
                }
                GFX.select(video)
                callback(video)
                RemsStudio.onLargeChange()
            }
            "Text" -> {
                try {
                    addText(name, parent, file.readText(), callback)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return
                }
            }
            "Mesh" -> {
                val mesh = Mesh(file, parent)
                mesh.file = file
                mesh.name = name
                GFX.select(mesh)
                callback(mesh)
                RemsStudio.onLargeChange()
            }
            "HTML" -> {
                // parse html? maybe, but html and css are complicated
                // rather use screenshots or svg...
                // integrated browser?
                println("todo html")
            }
            "Markdeep", "Markdown" -> {
                // execute markdeep script or interpret markdown to convert it to html? no
                // I see few use-cases
                println("todo markdeep")
            }
            else -> println("Unknown file type: ${file.extension}")
        }
    }
}

fun addText(name: String, parent: Transform?, text: String, callback: (Transform) -> Unit) {
    // important ;)
    // should maybe be done sometimes in object as well ;)
    if (text.length > 500) {
        addEvent {
            GFX.ask("Text has ${text.codePoints().count()} characters, import?") {
                val textNode = Text(text, parent)
                textNode.name = name
                GFX.select(textNode)
                callback(textNode)
                RemsStudio.onLargeChange()
            }
        }
        return
    }
    addEvent {
        val textNode = Text(text, parent)
        textNode.name = name
        GFX.select(textNode)
        callback(textNode)
        RemsStudio.onLargeChange()
    }
}