package me.anno.ui.editor.files

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.language.translation.NameDesc
import me.anno.objects.text.Text
import me.anno.objects.Transform
import me.anno.objects.Transform.Companion.toTransform
import me.anno.objects.Video
import me.anno.objects.meshes.Mesh
import me.anno.objects.modes.UVProjection
import me.anno.studio.rems.RemsStudio
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.studio.rems.Selection.selectTransform
import me.anno.ui.base.menu.Menu.ask
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
                            RemsStudio.largeChange("Added Folder"){
                                parent?.addChild(transform)
                                selectTransform(transform)
                                callback(transform)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    LOGGER.warn("Didn't understand JSON! ${e.message}")
                    addText(name, parent, text, callback)
                }
            }
            "Cubemap-Equ" -> {
                RemsStudio.largeChange("Added Cubemap"){
                    val cube = Video(file, parent)
                    cube.scale.set(Vector3f(1000f, 1000f, 1000f))
                    cube.uvProjection = UVProjection.Equirectangular
                    cube.name = name
                    selectTransform(cube)
                    callback(cube)
                }
            }
            "Cubemap-Tiles" -> {
                RemsStudio.largeChange("Added Cubemap"){
                    val cube = Video(file, parent)
                    cube.scale.set(Vector3f(1000f, 1000f, 1000f))
                    cube.uvProjection = UVProjection.TiledCubemap
                    cube.name = name
                    selectTransform(cube)
                    callback(cube)
                }
            }
            "Video", "Image", "Audio" -> {// the same, really ;)
                // rather use a list of keywords?
                RemsStudio.largeChange("Added Video"){
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
                    selectTransform(video)
                    callback(video)
                }
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
                RemsStudio.largeChange("Added Mesh"){
                    val mesh = Mesh(file, parent)
                    mesh.file = file
                    mesh.name = name
                    selectTransform(mesh)
                    callback(mesh)
                }
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
            ask(NameDesc("Text has %1 characters, import?", "", "obj.text.askLargeImport")
                .with("%1", text.codePoints().count().toString())) {
                RemsStudio.largeChange("Imported Text"){
                    val textNode = Text(text, parent)
                    textNode.name = name
                    selectTransform(textNode)
                    callback(textNode)
                }
            }
        }
        return
    }
    addEvent {
        RemsStudio.largeChange("Imported Text"){
            val textNode = Text(text, parent)
            textNode.name = name
            selectTransform(textNode)
            callback(textNode)
        }
    }
}